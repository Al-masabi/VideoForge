#include <jni.h>
#include <string.h>
#include <libavformat/avformat.h>
#include <libavcodec/avcodec.h>
#include <libavutil/opt.h>

JNIEXPORT jlongArray JNICALL
Java_com_videoforge_engine_ffmpeg_FfmpegBridge_nativeAnalyzeKeyframes(
        JNIEnv *env, jobject thiz, jstring path) {
    const char *path_str = (*env)->GetStringUTFChars(env, path, NULL);

    AVFormatContext *fmt_ctx = NULL;
    if (avformat_open_input(&fmt_ctx, path_str, NULL, NULL) < 0) {
        (*env)->ReleaseStringUTFChars(env, path, path_str);
        return NULL;
    }

    if (avformat_find_stream_info(fmt_ctx, NULL) < 0) {
        avformat_close_input(&fmt_ctx);
        (*env)->ReleaseStringUTFChars(env, path, path_str);
        return NULL;
    }

    int video_stream = -1;
    for (int i = 0; i < fmt_ctx->nb_streams; i++) {
        if (fmt_ctx->streams[i]->codecpar->codec_type == AVMEDIA_TYPE_VIDEO) {
            video_stream = i;
            break;
        }
    }

    if (video_stream < 0) {
        avformat_close_input(&fmt_ctx);
        (*env)->ReleaseStringUTFChars(env, path, path_str);
        return NULL;
    }

    int64_t *keyframes = NULL;
    int count = 0;
    int capacity = 1024;
    keyframes = av_malloc(capacity * sizeof(int64_t));

    AVPacket *pkt = av_packet_alloc();
    AVRational tb = fmt_ctx->streams[video_stream]->time_base;

    while (av_read_frame(fmt_ctx, pkt) >= 0) {
        if (pkt->stream_index == video_stream && (pkt->flags & AV_PKT_FLAG_KEY)) {
            if (count >= capacity) {
                capacity *= 2;
                keyframes = av_realloc(keyframes, capacity * sizeof(int64_t));
            }
            int64_t ms = av_rescale_q(pkt->pts, tb, (AVRational){1, 1000});
            keyframes[count++] = ms;
        }
        av_packet_unref(pkt);
    }

    av_packet_free(&pkt);
    avformat_close_input(&fmt_ctx);
    (*env)->ReleaseStringUTFChars(env, path, path_str);

    jlongArray result = (*env)->NewLongArray(env, count);
    (*env)->SetLongArrayRegion(env, result, 0, count, (jlong *)keyframes);
    av_free(keyframes);

    return result;
}

JNIEXPORT jboolean JNICALL
Java_com_videoforge_engine_ffmpeg_FfmpegBridge_nativeLosslessCut(
        JNIEnv *env, jobject thiz, jstring input_path, jstring output_path, jobjectArray segments) {
    const char *in_str = (*env)->GetStringUTFChars(env, input_path, NULL);
    const char *out_str = (*env)->GetStringUTFChars(env, output_path, NULL);

    AVFormatContext *in_ctx = NULL;
    AVFormatContext *out_ctx = NULL;

    if (avformat_open_input(&in_ctx, in_str, NULL, NULL) < 0) {
        (*env)->ReleaseStringUTFChars(env, input_path, in_str);
        (*env)->ReleaseStringUTFChars(env, output_path, out_str);
        return JNI_FALSE;
    }

    if (avformat_find_stream_info(in_ctx, NULL) < 0) {
        avformat_close_input(&in_ctx);
        (*env)->ReleaseStringUTFChars(env, input_path, in_str);
        (*env)->ReleaseStringUTFChars(env, output_path, out_str);
        return JNI_FALSE;
    }

    avformat_alloc_output_context2(&out_ctx, NULL, NULL, out_str);
    if (!out_ctx) {
        avformat_close_input(&in_ctx);
        (*env)->ReleaseStringUTFChars(env, input_path, in_str);
        (*env)->ReleaseStringUTFChars(env, output_path, out_str);
        return JNI_FALSE;
    }

    for (int i = 0; i < in_ctx->nb_streams; i++) {
        AVStream *out_stream = avformat_new_stream(out_ctx, NULL);
        avcodec_parameters_copy(out_stream->codecpar, in_ctx->streams[i]->codecpar);
        out_stream->codecpar->codec_tag = 0;
    }

    if (!(out_ctx->oformat->flags & AVFMT_NOFILE)) {
        if (avio_open(&out_ctx->pb, out_str, AVIO_FLAG_WRITE) < 0) {
            avformat_close_input(&in_ctx);
            avformat_free_context(out_ctx);
            (*env)->ReleaseStringUTFChars(env, input_path, in_str);
            (*env)->ReleaseStringUTFChars(env, output_path, out_str);
            return JNI_FALSE;
        }
    }

    avformat_write_header(out_ctx, NULL);

    jsize num_segments = (*env)->GetArrayLength(env, segments);
    AVPacket *pkt = av_packet_alloc();

    for (jsize s = 0; s < num_segments; s++) {
        jlongArray segment = (jlongArray)(*env)->GetObjectArrayElement(env, segments, s);
        jlong *segment_data = (*env)->GetLongArrayElements(env, segment, NULL);
        int64_t start_ms = segment_data[0];
        int64_t end_ms = segment_data[1];
        (*env)->ReleaseLongArrayElements(env, segment, segment_data, JNI_ABORT);

        int64_t start_us = start_ms * 1000;
        int64_t end_us = end_ms * 1000;

        av_seek_frame(in_ctx, -1, start_us, AVSEEK_FLAG_BACKWARD);

        while (av_read_frame(in_ctx, pkt) >= 0) {
            int64_t pts_us = av_rescale_q(pkt->pts,
                in_ctx->streams[pkt->stream_index]->time_base,
                (AVRational){1, 1000000});

            if (pts_us > end_us) {
                av_packet_unref(pkt);
                break;
            }

            if (pts_us >= start_us) {
                av_interleaved_write_frame(out_ctx, pkt);
            }

            av_packet_unref(pkt);
        }
    }

    av_write_trailer(out_ctx);
    av_packet_free(&pkt);

    if (!(out_ctx->oformat->flags & AVFMT_NOFILE)) {
        avio_closep(&out_ctx->pb);
    }

    avformat_close_input(&in_ctx);
    avformat_free_context(out_ctx);

    (*env)->ReleaseStringUTFChars(env, input_path, in_str);
    (*env)->ReleaseStringUTFChars(env, output_path, out_str);

    return JNI_TRUE;
}

JNIEXPORT jboolean JNICALL
Java_com_videoforge_engine_ffmpeg_FfmpegBridge_nativeEncodeCrf(
        JNIEnv *env, jobject thiz, jstring input_path, jstring output_path, jint crf, jstring speed) {
    const char *in_str = (*env)->GetStringUTFChars(env, input_path, NULL);
    const char *out_str = (*env)->GetStringUTFChars(env, output_path, NULL);
    const char *speed_str = (*env)->GetStringUTFChars(env, speed, NULL);

    AVFormatContext *in_ctx = NULL;
    AVFormatContext *out_ctx = NULL;

    if (avformat_open_input(&in_ctx, in_str, NULL, NULL) < 0) {
        (*env)->ReleaseStringUTFChars(env, input_path, in_str);
        (*env)->ReleaseStringUTFChars(env, output_path, out_str);
        (*env)->ReleaseStringUTFChars(env, speed, speed_str);
        return JNI_FALSE;
    }

    if (avformat_find_stream_info(in_ctx, NULL) < 0) {
        avformat_close_input(&in_ctx);
        (*env)->ReleaseStringUTFChars(env, input_path, in_str);
        (*env)->ReleaseStringUTFChars(env, output_path, out_str);
        (*env)->ReleaseStringUTFChars(env, speed, speed_str);
        return JNI_FALSE;
    }

    int video_stream = -1;
    for (int i = 0; i < in_ctx->nb_streams; i++) {
        if (in_ctx->streams[i]->codecpar->codec_type == AVMEDIA_TYPE_VIDEO) {
            video_stream = i;
            break;
        }
    }

    if (video_stream < 0) {
        avformat_close_input(&in_ctx);
        (*env)->ReleaseStringUTFChars(env, input_path, in_str);
        (*env)->ReleaseStringUTFChars(env, output_path, out_str);
        (*env)->ReleaseStringUTFChars(env, speed, speed_str);
        return JNI_FALSE;
    }

    AVCodecParameters *codecpar = in_ctx->streams[video_stream]->codecpar;
    const AVCodec *decoder = avcodec_find_decoder(codecpar->codec_id);
    AVCodecContext *dec_ctx = avcodec_alloc_context3(decoder);
    avcodec_parameters_to_context(dec_ctx, codecpar);
    avcodec_open2(dec_ctx, decoder, NULL);

    const AVCodec *encoder = avcodec_find_encoder_by_name("libx264");
    if (!encoder) {
        encoder = avcodec_find_encoder(AV_CODEC_ID_MPEG4);
    }

    AVCodecContext *enc_ctx = avcodec_alloc_context3(encoder);
    enc_ctx->width = dec_ctx->width;
    enc_ctx->height = dec_ctx->height;
    enc_ctx->pix_fmt = AV_PIX_FMT_YUV420P;
    enc_ctx->time_base = in_ctx->streams[video_stream]->time_base;
    enc_ctx->framerate = av_guess_frame_rate(in_ctx, in_ctx->streams[video_stream], NULL);

    char crf_str[16];
    snprintf(crf_str, sizeof(crf_str), "%d", crf);
    av_opt_set(enc_ctx->priv_data, "crf", crf_str, 0);
    av_opt_set(enc_ctx->priv_data, "preset", speed_str, 0);

    avcodec_open2(enc_ctx, encoder, NULL);

    avformat_alloc_output_context2(&out_ctx, NULL, NULL, out_str);

    AVStream *out_stream = avformat_new_stream(out_ctx, NULL);
    avcodec_parameters_from_context(out_stream->codecpar, enc_ctx);
    out_stream->time_base = enc_ctx->time_base;

    for (int i = 0; i < in_ctx->nb_streams; i++) {
        if (in_ctx->streams[i]->codecpar->codec_type == AVMEDIA_TYPE_AUDIO) {
            AVStream *audio_out = avformat_new_stream(out_ctx, NULL);
            avcodec_parameters_copy(audio_out->codecpar, in_ctx->streams[i]->codecpar);
            audio_out->codecpar->codec_tag = 0;
        }
    }

    if (!(out_ctx->oformat->flags & AVFMT_NOFILE)) {
        avio_open(&out_ctx->pb, out_str, AVIO_FLAG_WRITE);
    }

    avformat_write_header(out_ctx, NULL);

    AVPacket *pkt = av_packet_alloc();
    AVFrame *frame = av_frame_alloc();
    AVPacket *enc_pkt = av_packet_alloc();

    while (av_read_frame(in_ctx, pkt) >= 0) {
        if (pkt->stream_index == video_stream) {
            avcodec_send_packet(dec_ctx, pkt);

            while (avcodec_receive_frame(dec_ctx, frame) >= 0) {
                avcodec_send_frame(enc_ctx, frame);

                while (avcodec_receive_packet(enc_ctx, enc_pkt) >= 0) {
                    av_interleaved_write_frame(out_ctx, enc_pkt);
                    av_packet_unref(enc_pkt);
                }
            }
        } else {
            av_interleaved_write_frame(out_ctx, pkt);
        }

        av_packet_unref(pkt);
    }

    avcodec_send_frame(enc_ctx, NULL);
    while (avcodec_receive_packet(enc_ctx, enc_pkt) >= 0) {
        av_interleaved_write_frame(out_ctx, enc_pkt);
        av_packet_unref(enc_pkt);
    }

    av_write_trailer(out_ctx);

    av_packet_free(&pkt);
    av_frame_free(&frame);
    av_packet_free(&enc_pkt);
    avcodec_free_context(&dec_ctx);
    avcodec_free_context(&enc_ctx);

    if (!(out_ctx->oformat->flags & AVFMT_NOFILE)) {
        avio_closep(&out_ctx->pb);
    }

    avformat_close_input(&in_ctx);
    avformat_free_context(out_ctx);

    (*env)->ReleaseStringUTFChars(env, input_path, in_str);
    (*env)->ReleaseStringUTFChars(env, output_path, out_str);
    (*env)->ReleaseStringUTFChars(env, speed, speed_str);

    return JNI_TRUE;
}