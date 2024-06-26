<script setup>
import { ref, computed, onMounted } from 'vue';
import axios from 'axios';
import moment from 'moment-timezone';

const isStartMenuOpen = ref(false);
const selectedStartDate = ref(null);
const tempStartDate = ref(null);

const isEndMenuOpen = ref(false);
const selectedEndDate = ref(null);
const tempEndDate = ref(null);

const currentScore = ref(0);
const previousScore = ref(0);
const currentScoreMessage = ref('');
const previousScoreMessage = ref('');
const currentTimePeriod = ref('');
const previousTimePeriod = ref('');
const scoreChangeMessage = ref('');

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

const formatPeriod = (period) => {
  const [startDate, endDate] = period.split(' to ');
  return `${formatDateString(new Date(startDate))} - ${formatDateString(new Date(endDate))}`;
};

const resetResults = () => {
  currentScore.value = 0;
  previousScore.value = 0;
  currentScoreMessage.value = '';
  previousScoreMessage.value = '';
  currentTimePeriod.value = '';
  previousTimePeriod.value = '';
  scoreChangeMessage.value = '';
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

const fetchScores = async () => {
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
          includePreviousPeriod: true,
        },
        headers: {
          [headerKey]: import.meta.env.VITE_APP_API_KEY,
        },
      },
    );

    const { currentPeriodScore, previousPeriodScore, scoreChange } =
      response.data;

    currentScore.value = parseFloat(
      currentPeriodScore.averageScorePercentage.toFixed(2),
    );
    currentScoreMessage.value = currentPeriodScore.message || '';
    currentTimePeriod.value = formatPeriod(currentPeriodScore.period);
    previousScore.value = parseFloat(
      previousPeriodScore.averageScorePercentage.toFixed(2),
    );
    previousScoreMessage.value = previousPeriodScore.message || '';
    previousTimePeriod.value = formatPeriod(previousPeriodScore.period);

    scoreChangeMessage.value =
      currentScoreMessage.value === 'N/A' ||
      previousScoreMessage.value === 'N/A'
        ? 'N/A'
        : `${scoreChange.value.toFixed(2)}%`;
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
  fetchScores();
};

const setEndDateAndClose = () => {
  const date = new Date(tempEndDate.value);
  date.setHours(23, 59, 59, 999);
  selectedEndDate.value = moment(date)
    .tz('Europe/Tallinn')
    .format('YYYY-MM-DDTHH:mm:ss');
  isEndMenuOpen.value = false;
  fetchScores();
};

onMounted(fetchScores);

const getGradientColor = (score, message) => {
  if (message) return 'rgb(209, 213, 219)'; // Gray if N/A

  const startColor = { r: 252, g: 165, b: 165 };
  const endColor = { r: 134, g: 239, b: 172 };
  const percentage = score / 100;

  const r = Math.round(startColor.r + (endColor.r - startColor.r) * percentage);
  const g = Math.round(startColor.g + (endColor.g - startColor.g) * percentage);
  const b = Math.round(startColor.b + (endColor.b - startColor.b) * percentage);

  return `rgb(${r}, ${g}, ${b})`;
};

const currentGradientColor = computed(() =>
  getGradientColor(currentScore.value, currentScoreMessage.value),
);
const previousGradientColor = computed(() =>
  getGradientColor(previousScore.value, previousScoreMessage.value),
);

const changeColor = computed(() => {
  const change = parseFloat(scoreChangeMessage.value);
  if (scoreChangeMessage.value === 'N/A') {
    return 'rgb(209, 213, 219)'; // Gray
  }
  if (change > 0) {
    return 'rgb(22, 163, 74)'; // Green
  }
  if (change < 0) {
    return 'rgb(220, 38, 38)'; // Red
  }
  return 'rgb(202, 138, 4)'; // Yellow
});

const isDataLoaded = computed(
  () => currentTimePeriod.value && previousTimePeriod.value,
);
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
              >Period Over Period Quality Score Change</v-toolbar-title
            >
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
              Select a date range to view the scores
            </div>
            <div v-if="loading" class="text-center">
              <v-progress-circular
                indeterminate
                color="rgb(209, 213, 219)"
              ></v-progress-circular>
            </div>
            <div v-else-if="isDataLoaded" class="score-display">
              <div class="score-container">
                <v-progress-circular
                  :model-value="previousScoreMessage ? 0 : previousScore"
                  size="150"
                  width="15"
                  :color="previousGradientColor"
                  class="score-circle"
                >
                  <template #default>
                    <div class="score-text">
                      {{ previousScoreMessage || `${previousScore}%` }}
                    </div>
                  </template>
                </v-progress-circular>
                <div class="time-period">{{ previousTimePeriod }}</div>
              </div>
              <div class="score-change">
                <div
                  :style="{ color: changeColor, whiteSpace: 'nowrap' }"
                  class="change-text"
                >
                  {{ scoreChangeMessage }}
                </div>
                <div class="change-label">Change</div>
              </div>
              <div class="score-container">
                <v-progress-circular
                  :model-value="currentScoreMessage ? 0 : currentScore"
                  size="150"
                  width="15"
                  :color="currentGradientColor"
                  class="score-circle"
                >
                  <template #default>
                    <div class="score-text">
                      {{ currentScoreMessage || `${currentScore}%` }}
                    </div>
                  </template>
                </v-progress-circular>
                <div class="time-period">{{ currentTimePeriod }}</div>
              </div>
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
  flex-direction: row;
  justify-content: center;
  align-items: center;
  height: 18.75rem;
  width: 100%;
}

.circle-container-placeholder {
  height: 18.75rem;
  width: 100%;
}

.score-display {
  display: flex;
  flex-direction: row;
  justify-content: center;
  align-items: center;
  width: 100%;
}

.score-container {
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  margin: 0 1.25rem;
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

.score-change {
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  margin: 0 1.25rem;
  text-align: center;
}

.change-text {
  font-size: 2rem;
  font-weight: bold;
}

.change-label {
  font-size: 1rem;
  color: gray;
}

.placeholder {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100%;
  color: gray;
  font-size: 1.25rem;
}

.custom-alert {
  background-color: rgb(239, 68, 68) !important;
  color: white !important;
}

@media (max-width: 600px) {
  .circle-container {
    flex-direction: column;
    padding: 0;
    margin: 1rem 0;
  }

  .score-display {
    flex-direction: column;
  }

  .score-container,
  .score-change {
    margin: 0.625rem 0;
  }
}
</style>
