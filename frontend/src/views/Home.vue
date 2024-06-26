<script setup>
import { computed, ref } from 'vue';
import AggregatedScores from '../components/AggregatedScores.vue';
import TicketCategoryScores from '../components/TicketCategoryScores.vue';
import WeightedScoresWithoutPrevious from '../components/WeightedScoresWithoutPrevious.vue';
import WeightedScoresWithPrevious from '../components/WeightedScoresWithPrevious.vue';

const componentOptions = [
  {
    name: 'Aggregated Category Scores Over a Period of Time',
    component: AggregatedScores,
  },
  { name: 'Category Scores by Ticket ID', component: TicketCategoryScores },
  {
    name: 'Overall Quality Score for a Period',
    component: WeightedScoresWithoutPrevious,
  },
  {
    name: 'Period Over Period Quality Score Change',
    component: WeightedScoresWithPrevious,
  },
];

const selectedComponentName = ref(null);

const currentComponent = computed(() => {
  const selectedOption = componentOptions.find(
    (option) => option.name === selectedComponentName.value,
  );
  return selectedOption ? selectedOption.component : null;
});
</script>

<template>
  <v-container>
    <v-select
      v-model="selectedComponentName"
      :items="componentOptions.map((option) => option.name)"
      label="Select a dashboard"
      class="mb-4"
      clearable
      variant="outlined"
    ></v-select>

    <v-card class="mt-4 p-4" rounded="lg">
      <component :is="currentComponent"></component>
    </v-card>
  </v-container>
</template>

<style scoped>
.mt-4 {
  margin-top: 1rem;
}

.mb-4 {
  margin-bottom: 1rem;
}
</style>
