import React, { useRef, useEffect } from 'react';
import { createApp, h } from 'vue';
import Shop from '../../microclient-vue/src/components/Shop.vue';

import 'vuetify/styles';
import { createVuetify } from 'vuetify';
import * as components from 'vuetify/components';
import * as directives from 'vuetify/directives';
import '@mdi/font/css/materialdesignicons.css';

const VueShopWrapper = () => {
  const vueRoot = useRef(null);

  useEffect(() => {
    if (vueRoot.current && !vueRoot.current.hasChildNodes()) {
      const vuetify = createVuetify({
        components,
        directives,
      });

      const App = {
        render() {
          return h(Shop);
        },
      };

      const app = createApp(App);
      app.use(vuetify);
      app.mount(vueRoot.current);
    }
  }, []);

  return <div ref={vueRoot}></div>;
};

export default VueShopWrapper;
