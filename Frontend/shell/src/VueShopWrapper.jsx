import React, { useRef, useEffect } from 'react';
import { createApp } from 'vue';
import Shop from '../../microclient-vue/src/components/Shop.vue';

// Import Vuetify and its styles
import 'vuetify/styles';
import { createVuetify } from 'vuetify';
import * as components from 'vuetify/components';
import * as directives from 'vuetify/directives';
import '@mdi/font/css/materialdesignicons.css';

const VueShopWrapper = () => {
  const vueRoot = useRef(null);

  useEffect(() => {
    if (vueRoot.current && !vueRoot.current.hasChildNodes()) {
      // Create a Vuetify instance
      const vuetify = createVuetify({
        components,
        directives,
      });

      // Create a Vue app instance, use Vuetify, and then mount the component
      const app = createApp(Shop);
      app.use(vuetify);
      app.mount(vueRoot.current);
    }
  }, []);

  return <div ref={vueRoot}></div>;
};

export default VueShopWrapper;
