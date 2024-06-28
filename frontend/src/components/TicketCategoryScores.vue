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

const ticketData = ref([]);
const searchQuery = ref('');
const page = ref(1);
const itemsPerPage = ref(10);
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
    ticketData.value = [];
    return;
  }

  loading.value = true;
  errorAlert.value = false;

  try {
    const apiBaseUrl = import.meta.env.VITE_APP_API_BASE_URL;
    const headerKey = import.meta.env.VITE_APP_API_KEY_HEADER;

    const response = await axios.get(
      `${apiBaseUrl}/api/v1/tickets/category-scores`,
      {
        params: {
          startDate: selectedStartDate.value,
          endDate: selectedEndDate.value,
        },
        headers: {
          [headerKey]: import.meta.env.VITE_APP_API_KEY,
        },
      },
    );

    if (!response.data.length) {
      errorMessage.value = 'The query result was empty';
      errorAlert.value = true;
      loading.value = false;
      ticketData.value = [];
      return;
    }

    ticketData.value = response.data.map((item) => ({
      ticketId: item.TicketId,
      spelling: item.CategoryScores.Spelling,
      randomness: item.CategoryScores.Randomness,
      grammar: item.CategoryScores.Grammar,
      gdpr: item.CategoryScores.GDPR,
    }));
  } catch (error) {
    errorMessage.value =
      error.response?.status === 404
        ? 'The query result was empty'
        : `Error fetching data: ${error.message}`;
    errorAlert.value = true;
    ticketData.value = [];
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

const filteredData = computed(() => {
  if (!searchQuery.value) return ticketData.value;
  return ticketData.value.filter((item) =>
    item.ticketId.toString().startsWith(searchQuery.value),
  );
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
              >Category Scores by Ticket ID</v-toolbar-title
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
          <v-divider class="mx-4" inset vertical></v-divider>
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
          <div v-else>
            <v-text-field
              v-model="searchQuery"
              label="Search by Ticket ID"
              class="mb-4"
              outlined
              dense
            ></v-text-field>
            <div class="table-wrapper">
              <v-data-table
                v-model:page="page"
                :items="filteredData"
                :items-per-page="itemsPerPage"
                class="elevation-1 no-vertical-borders"
                item-value="ticketId"
              >
                <template #headers>
                  <tr>
                    <th class="bold-header text-left">Ticket ID</th>
                    <th class="bold-header text-center">Spelling</th>
                    <th class="bold-header text-center">Randomness</th>
                    <th class="bold-header text-center">Grammar</th>
                    <th class="bold-header text-center">GDPR</th>
                  </tr>
                </template>
                <template #item="{ item }">
                  <tr>
                    <td class="text-left">{{ item.ticketId }}</td>
                    <td class="text-center">{{ item.spelling.toFixed(2) }}%</td>
                    <td class="text-center">
                      {{ item.randomness.toFixed(2) }}%
                    </td>
                    <td class="text-center">{{ item.grammar.toFixed(2) }}%</td>
                    <td class="text-center">{{ item.gdpr.toFixed(2) }}%</td>
                  </tr>
                </template>
              </v-data-table>
            </div>
          </div>
        </v-card>
      </v-col>
    </v-row>
  </v-container>
</template>

<style scoped>
.bold-header {
  font-weight: bold !important;
}

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

.no-vertical-borders th,
.no-vertical-borders td {
  border-left: none !important;
  border-right: none !important;
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
  overflow-x: auto;
  margin: 2rem 2rem;
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
  max-width: 100%;
  min-width: 50%;
}

.circle-container {
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  height: 17.25rem;
  width: 100%;
}

.placeholder {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100%;
  color: gray;
  font-size: 1.25rem;
  margin-bottom: 1.5rem;
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
</style>
