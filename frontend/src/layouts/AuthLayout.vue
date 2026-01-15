<script setup>
import { RouterView } from 'vue-router'
</script>

<template>
  <div class="min-h-screen flex items-center justify-center bg-gradient-to-br from-primary-600 to-primary-800 py-12 px-4 sm:px-6 lg:px-8">
    <div class="max-w-md w-full">
      <RouterView />
    </div>
  </div>
</template>
