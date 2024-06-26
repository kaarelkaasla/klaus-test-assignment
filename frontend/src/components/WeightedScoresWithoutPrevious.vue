<script setup>
import { computed, onMounted, ref } from 'vue';
import axios from 'axios';
import moment from 'moment-timezone';

const isStartMenuOpen = ref(false);
const selectedStartDate = ref(null);
const tempStartDate = ref(null);

const isEndMenuOpen = ref(false);
const selectedEndDate = ref(null);
const tempEndDate = ref(null);

const score = ref(0);
const scoreMessage = ref('');
const timePeriod = ref('');

const loading = ref(false);
const errorAlert = ref(false);
const errorMessage = ref('');

const formatDateString = (date) => {
  const options = {
    weekday: 'short',
    year: 'numeric',
    month: 'short',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
    hour12: false,
    timeZoneName: 'short',
  };
  return new Intl.DateTimeFormat('en-US', options).format(date);
};

const formattedStartDate = computed(() =>
  selectedStartDate.value
    ? formatDateString(new Date(selectedStartDate.value))
    : '',
);
const formattedEndDate = computed(() =>
  selectedEndDate.value
    ? formatDateString(new Date(selectedEndDate.value))
    : '',
);

const resetResults = () => {
  score.value = 0;
  scoreMessage.value = '';
  timePeriod.value = '';
};

const handleError = (error) => {
  if (error.message === 'timeout') {
    errorMessage.value = 'The API request timed out';
  } else if (error.response && error.response.status === 404) {
    errorMessage.value = 'The query result was empty';
  } else {
    errorMessage.value = `Error fetching data: ${error.message}`;
  }
  errorAlert.value = true;
  resetResults();
};

const fetchScore = async () => {
  if (!selectedStartDate.value || !selectedEndDate.value) return;

  if (new Date(selectedStartDate.value) > new Date(selectedEndDate.value)) {
    errorMessage.value = 'The start date must be before the end date';
    errorAlert.value = true;
    resetResults();
    return;
  }

  loading.value = true;
  errorAlert.value = false;

  try {
    const apiBaseUrl = import.meta.env.VITE_APP_API_BASE_URL;
    const headerKey = import.meta.env.VITE_APP_API_KEY_HEADER;

    const response = await axios.get(
      `${apiBaseUrl}/api/v1/tickets/weighted-scores`,
      {
        params: {
          startDate: selectedStartDate.value,
          endDate: selectedEndDate.value,
          includePreviousPeriod: false,
        },
        headers: {
          [headerKey]: import.meta.env.VITE_APP_API_KEY,
        },
      },
    );

    const { currentPeriodScore } = response.data;

    if (!currentPeriodScore) {
      errorMessage.value = 'The query result was empty';
      errorAlert.value = true;
      resetResults();
      return;
    }

    score.value = parseFloat(
      currentPeriodScore.averageScorePercentage.toFixed(2),
    );
    scoreMessage.value = currentPeriodScore.message || '';
    timePeriod.value = `${formattedStartDate.value} - ${formattedEndDate.value}`;
  } catch (error) {
    handleError(error);
  } finally {
    loading.value = false;
  }
};

const setStartDateAndClose = () => {
  const date = new Date(tempStartDate.value);
  date.setHours(0, 0, 0, 0);
  selectedStartDate.value = moment(date)
    .tz('Europe/Tallinn')
    .format('YYYY-MM-DDTHH:mm:ss');
  isStartMenuOpen.value = false;
  fetchScore();
};

const setEndDateAndClose = () => {
  const date = new Date(tempEndDate.value);
  date.setHours(23, 59, 59, 999);
  selectedEndDate.value = moment(date)
    .tz('Europe/Tallinn')
    .format('YYYY-MM-DDTHH:mm:ss');
  isEndMenuOpen.value = false;
  fetchScore();
};

onMounted(fetchScore);

const gradientColor = computed(() => {
  const startColor = { r: 252, g: 165, b: 165 };
  const endColor = { r: 134, g: 239, b: 172 };
  const percentage = score.value / 100;

  const r = Math.round(startColor.r + (endColor.r - startColor.r) * percentage);
  const g = Math.round(startColor.g + (endColor.g - startColor.g) * percentage);
  const b = Math.round(startColor.b + (endColor.b - startColor.b) * percentage);

  return `rgb(${r}, ${g}, ${b})`;
});
</script>

<template>
  <v-container fluid>
    <v-row justify="center">
      <v-col cols="12">
        <v-alert
          v-model="errorAlert"
          type="error"
          closable
          class="custom-alert"
        >
          {{ errorMessage }}
        </v-alert>
        <v-card class="mt-4 p-4" rounded="lg">
          <v-toolbar flat>
            <v-toolbar-title class="bold-title"
              >Overall Quality Score for a Period
            </v-toolbar-title>
          </v-toolbar>
          <v-row>
            <v-col cols="12" md="6">
              <v-menu
                ref="startMenu"
                v-model="isStartMenuOpen"
                :close-on-content-click="false"
              >
                <template #activator="{ props }">
                  <v-text-field
                    v-bind="props"
                    ref="startActivator"
                    v-model="formattedStartDate"
                    label="Start date"
                    readonly
                  ></v-text-field>
                </template>
                <v-date-picker v-model="tempStartDate">
                  <template #actions>
                    <v-btn text @click="isStartMenuOpen = false">Cancel</v-btn>
                    <v-btn text @click="setStartDateAndClose">OK</v-btn>
                  </template>
                </v-date-picker>
              </v-menu>
            </v-col>
            <v-col cols="12" md="6">
              <v-menu
                ref="endMenu"
                v-model="isEndMenuOpen"
                :close-on-content-click="false"
              >
                <template #activator="{ props }">
                  <v-text-field
                    v-bind="props"
                    ref="endActivator"
                    v-model="formattedEndDate"
                    label="End date"
                    readonly
                  ></v-text-field>
                </template>
                <v-date-picker v-model="tempEndDate">
                  <template #actions>
                    <v-btn text @click="isEndMenuOpen = false">Cancel</v-btn>
                    <v-btn text @click="setEndDateAndClose">OK</v-btn>
                  </template>
                </v-date-picker>
              </v-menu>
            </v-col>
          </v-row>
          <div class="circle-container">
            <div
              v-if="!selectedStartDate || !selectedEndDate"
              class="placeholder"
            >
              Select a date range to view the score
            </div>
            <div v-else-if="loading" class="text-center">
              <v-progress-circular
                indeterminate
                color="rgb(209, 213, 219)"
              ></v-progress-circular>
            </div>
            <div v-else-if="!errorAlert && timePeriod" class="score-display">
              <v-progress-circular
                :model-value="scoreMessage ? 0 : score"
                size="150"
                width="15"
                :color="gradientColor"
                class="score-circle"
              >
                <template #default>
                  <div class="score-text">
                    {{ scoreMessage || `${score}%` }}
                  </div>
                </template>
              </v-progress-circular>
              <div class="time-period">{{ timePeriod }}</div>
            </div>
          </div>
        </v-card>
      </v-col>
    </v-row>
  </v-container>
</template>

<style scoped>
.bold-title {
  font-weight: bold !important;
}

.circle-container {
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  height: 18.75rem;
  width: 100%;
}

.placeholder {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100%;
  color: gray;
  font-size: 1.25rem;
}

.score-display {
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  width: 100%;
}

.score-circle {
  display: flex;
  justify-content: center;
  align-items: center;
}

.score-text {
  font-size: 1.5rem;
  padding: 0.625rem;
  font-weight: bold;
}

.time-period {
  margin-top: 1rem;
  font-size: 1.25rem;
  font-weight: bold;
  color: gray;
  text-align: center;
}

@media (max-width: 600px) {
  .circle-container {
    padding: 0;
    margin: 1rem 0;
  }

  .score-display {
    flex-direction: column;
  }
}

.custom-alert {
  background-color: rgb(239, 68, 68) !important;
  color: white !important;
}
</style>
