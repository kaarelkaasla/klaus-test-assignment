import { createVuetify } from 'vuetify';
import 'vuetify/styles';
import * as components from 'vuetify/components';
import * as directives from 'vuetify/directives';
import { aliases, mdi } from 'vuetify/iconsets/mdi';
import DayJsAdapter from '@date-io/dayjs';

const vuetify = createVuetify({
  components: {
    ...components,
    VDatePicker: components.VDatePicker,
  },
  directives,
  date: {
    adapter: DayJsAdapter,
  },
  icons: {
    defaultSet: 'mdi',
    aliases,
    sets: { mdi },
  },
  theme: {
    themes: {
      light: {
        colors: {
          primary: '#6200ea',
          secondary: '#03dac6',
          accent: '#03a9f4',
          error: '#f44336',
          info: '#2196f3',
          success: '#4caf50',
          warning: '#fb8c00',
          background: '#f0f0f0',
        },
      },
    },
  },
});

export default vuetify;
