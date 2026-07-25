package com.videoforge.android.di

import com.videoforge.android.plugin.PluginRegistry
import com.videoforge.android.plugin.PluginSubtitleTransformer
import com.videoforge.core.data.subtitle.SubtitleCueTransformer
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet

@Module
@InstallIn(SingletonComponent::class)
object PluginBindingsModule {

    @Provides
    @IntoSet
    fun providePluginSubtitleTransformer(
        pluginRegistry: PluginRegistry
    ): SubtitleCueTransformer {
        return PluginSubtitleTransformer(pluginRegistry)
    }
}