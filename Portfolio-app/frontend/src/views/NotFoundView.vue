<script setup lang="ts">
import { useRouter } from 'vue-router';

const router = useRouter();
</script>

<template>
  <div class="not-found">
    <div class="content">
      <h1>404</h1>
      <h2>Page Not Found</h2>
      <p>The page you're looking for doesn't exist or has been moved.</p>
      <button class="btn btn-primary" @click="router.push('/dashboard')">
        Go to Dashboard
      </button>
    </div>
  </div>
</template>

<style scoped>
.not-found {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: var(--bg-primary);
}

.content {
  text-align: center;
  padding: 24px;
}

h1 {
  font-size: 6rem;
  font-weight: 700;
  color: var(--text-muted);
  margin-bottom: 8px;
}

h2 {
  font-size: 1.5rem;
  margin-bottom: 12px;
}

p {
  color: var(--text-secondary);
  margin-bottom: 24px;
}
</style>
