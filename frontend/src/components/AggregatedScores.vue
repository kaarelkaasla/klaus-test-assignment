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

const categoryScores = ref({});
const headers = ref([]);
const data = ref([]);
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

const formattedStartDate = computed(() => {
  return selectedStartDate.value
    ? formatDateString(new Date(selectedStartDate.value))
    : '';
});

const formattedEndDate = computed(() => {
  return selectedEndDate.value
    ? formatDateString(new Date(selectedEndDate.value))
    : '';
});

const fetchData = async () => {
  if (!selectedStartDate.value || !selectedEndDate.value) return;

  if (new Date(selectedStartDate.value) > new Date(selectedEndDate.value)) {
    errorMessage.value = 'The start date must be before the end date';
    errorAlert.value = true;
    loading.value = false;
    data.value = [];
    headers.value = [];
    return;
  }

  loading.value = true;
  errorAlert.value = false;

  try {
    const apiBaseUrl = import.meta.env.VITE_APP_API_BASE_URL;
    const headerKey = import.meta.env.VITE_APP_API_KEY_HEADER;

    const response = await axios.get(`${apiBaseUrl}/api/v1/scores/aggregated`, {
      params: {
        startDate: selectedStartDate.value,
        endDate: selectedEndDate.value,
      },
      headers: {
        [headerKey]: import.meta.env.VITE_APP_API_KEY,
      },
    });

    const scores = response.data.categoryRatingResults;
    if (!scores || !scores.length) {
      errorMessage.value = 'The query result was empty';
      errorAlert.value = true;
      data.value = [];
      headers.value = [];
      loading.value = false;
      return;
    }

    categoryScores.value = scores;

    headers.value = [
      'Category',
      'Ratings',
      ...scores[0].periodScores.map((score) => score.period),
      'Score',
    ];
    data.value = scores.map((categoryResult) => {
      const periodScores = categoryResult.periodScores.map(
        (score) => `${score.averageScorePercentage.toFixed(2)}%`,
      );
      return [
        categoryResult.categoryName,
        categoryResult.frequency,
        ...periodScores,
        `${categoryResult.overallAverageScorePercentage.toFixed(2)}%`,
      ];
    });
  } catch (error) {
    errorMessage.value =
      error.response?.status === 404
        ? 'The query result was empty'
        : `Error fetching data: ${error.message}`;
    errorAlert.value = true;
    data.value = [];
    headers.value = [];
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
  fetchData();
};

const setEndDateAndClose = () => {
  const date = new Date(tempEndDate.value);
  date.setHours(23, 59, 59, 999);
  selectedEndDate.value = moment(date)
    .tz('Europe/Tallinn')
    .format('YYYY-MM-DDTHH:mm:ss');
  isEndMenuOpen.value = false;
  fetchData();
};

onMounted(fetchData);
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
              >Aggregated Category Scores Over a Period of Time</v-toolbar-title
            >
          </v-toolbar>
          <v-row>
            <v-col cols="12" md="6">
              <!-- Start Date Picker -->
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
              <!-- End Date Picker -->
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
          <div class="table-wrapper">
            <div
              v-if="
                !selectedStartDate || !selectedEndDate || loading || errorAlert
              "
              class="circle-container"
            >
              <div
                v-if="!selectedStartDate || !selectedEndDate"
                class="placeholder"
              >
                Select a date range to view the data
              </div>
              <div v-else-if="loading" class="text-center">
                <v-progress-circular
                  indeterminate
                  color="rgb(209, 213, 219)"
                ></v-progress-circular>
              </div>
            </div>
            <div v-else class="table-container">
              <v-simple-table fixed>
                <thead>
                  <tr>
                    <th
                      v-for="(header, index) in headers"
                      :key="header"
                      :class="
                        index === 0
                          ? 'text-left py-2 px-4 nowrap'
                          : 'text-center py-2 px-4 nowrap'
                      "
                    >
                      {{ header }}
                    </th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="row in data" :key="row[0]">
                    <td
                      v-for="(cell, index) in row"
                      :key="index"
                      :class="index === 0 ? 'text-left' : 'text-center'"
                      class="py-2 px-4 border-b nowrap"
                    >
                      {{ cell }}
                    </td>
                  </tr>
                </tbody>
              </v-simple-table>
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

.text-center {
  text-align: center;
}

.text-left {
  text-align: left;
}

.border-b {
  border-bottom: 1px solid #e0e0e0;
}

.py-2 {
  padding-top: 0.5rem;
  padding-bottom: 0.5rem;
}

.px-4 {
  padding-left: 1rem;
  padding-right: 1rem;
}

.table-wrapper {
  padding: 0 1rem;
  overflow-x: auto;
  margin: 2rem 2rem;
  display: flex;
  justify-content: flex-start;
  align-items: center;
  min-height: 14.75rem;
}

.table-container {
  min-width: max-content;
}

.v-card {
  width: 100%;
  box-sizing: border-box;
}

.v-container {
  max-width: 100%;
  padding: 1rem 1rem;
  box-sizing: border-box;
}

.nowrap {
  white-space: nowrap;
}

th,
td {
  padding: 0.75rem;
}

.v-simple-table {
  width: 100%;
}

.circle-container {
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  height: 100%;
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

.table-display {
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  width: 100%;
}

.custom-alert {
  background-color: rgb(239, 68, 68) !important;
  color: white !important;
}

@media (max-width: 600px) {
  .table-wrapper {
    padding: 0;
    margin: 1rem 0;
  }
}
</style>
