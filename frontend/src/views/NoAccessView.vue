<script setup>
import { RouterLink } from 'vue-router'
import { HomeIcon, ShieldExclamationIcon } from '@heroicons/vue/24/outline'
</script>

<template>
  <div class="min-h-screen flex items-center justify-center bg-gray-50 py-12 px-4 sm:px-6 lg:px-8">
    <div class="max-w-md w-full text-center">
      <ShieldExclamationIcon class="mx-auto h-24 w-24 text-red-300" />
      <h1 class="mt-4 text-6xl font-bold text-gray-200">403</h1>
      <h2 class="mt-4 text-3xl font-bold text-gray-900">Access Denied</h2>
      <p class="mt-2 text-gray-500">
        You don't have permission to access this page. Contact your administrator if you believe this is an error.
      </p>
      <div class="mt-6">
        <RouterLink to="/dashboard" class="btn-primary inline-flex items-center">
          <HomeIcon class="h-5 w-5 mr-2" />
          Back to Dashboard
        </RouterLink>
      </div>
    </div>
  </div>
</template>
