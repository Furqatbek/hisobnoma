<script setup>
import { RouterLink } from 'vue-router'
import { HomeIcon } from '@heroicons/vue/24/outline'
</script>

<template>
  <div class="min-h-screen flex items-center justify-center bg-gray-50 py-12 px-4 sm:px-6 lg:px-8">
    <div class="max-w-md w-full text-center">
      <h1 class="text-9xl font-bold text-gray-200">404</h1>
      <h2 class="mt-4 text-3xl font-bold text-gray-900">{{ $t('auth.pageNotFound') }}</h2>
      <p class="mt-2 text-gray-500">
        {{ $t('auth.pageNotFoundText') }}
      </p>
      <div class="mt-6">
        <RouterLink to="/dashboard" class="btn-primary inline-flex items-center">
          <HomeIcon class="h-5 w-5 mr-2" />
          {{ $t('auth.backToDashboard') }}
        </RouterLink>
      </div>
    </div>
  </div>
</template>
