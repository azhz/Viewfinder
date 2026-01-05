<template>
  <div class="search-bar">
    <div class="search-input-container">
      <input
        v-model="searchKeyword"
        type="text"
        placeholder="搜索照片、用户或地点..."
        class="search-input"
        @keyup.enter="performSearch"
      />
      <button @click="performSearch" class="search-button">
        <span>🔍</span>
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()
const searchKeyword = ref('')

const performSearch = () => {
  if (searchKeyword.value.trim()) {
    router.push({ name: 'SearchResults', params: { keyword: searchKeyword.value } })
  }
}
</script>

<style scoped>
.search-bar {
  width: 100%;
  padding: 1rem 0;
}

.search-input-container {
  display: flex;
  border: 1px solid #ddd;
  border-radius: 24px;
  overflow: hidden;
  max-width: 600px;
  margin: 0 auto;
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
}

.search-input {
  flex: 1;
  padding: 0.75rem 1rem;
  border: none;
  outline: none;
  font-size: 1rem;
}

.search-button {
  padding: 0.75rem 1rem;
  background: #007bff;
  color: white;
  border: none;
  cursor: pointer;
  font-size: 1.2rem;
}

.search-button:hover {
  background: #0056b3;
}
</style>