package com.nzrbits.hush.core.system.di

import com.nzrbits.hush.core.common.AppDispatchers
import com.nzrbits.hush.core.common.time.HushClock
import com.nzrbits.hush.core.common.time.SystemClock
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SystemModule {
    @Provides @Singleton fun clock(): HushClock = SystemClock()
    @Provides @Singleton fun dispatchers(): AppDispatchers = AppDispatchers()
}
