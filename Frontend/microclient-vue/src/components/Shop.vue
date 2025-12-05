<template>
  <v-app theme="dark">
    <v-container class="py-8">
      <v-row justify="center">
        <v-col cols="12" md="10" lg="8">
          <header class="text-center mb-8">
            <h1 class="text-h3 font-weight-bold text-purple-accent-1">
              Snack & Drink Stop
            </h1>
            <p class="text-medium-emphasis">Your favorite snacks and drinks, delivered fast</p>
          </header>
          <v-card
              v-if="!user"
              class="pa-6 mx-auto"
              max-width="450"
              elevation="12"
              rounded="lg"
          >
            <v-card-title class="text-h5 text-center mb-4">
              Welcome!
            </v-card-title>
            <v-card-text>
              <v-text-field
                  v-model="form.username"
                  label="Username"
                  prepend-inner-icon="mdi-account-outline"
                  variant="outlined"
                  required
                  class="mb-3"
              />
              <v-text-field
                  v-model="form.password"
                  type="password"
                  label="Password"
                  prepend-inner-icon="mdi-lock-outline"
                  variant="outlined"
                  required
              />
              <v-btn
                  class="mt-4"
                  color="purple-accent-1"
                  @click="login"
                  block
                  size="large"
              >
                Login
              </v-btn>

              <v-btn
                  class="mt-3"
                  variant="tonal"
                  @click="registerUser"
                  block
              >
                Register
              </v-btn>
              <v-alert
                  v-if="msg"
                  class="mt-5"
                  :type="user ? 'success' : 'error'"
                  variant="tonal"
                  density="compact"
                  border="start"
              >
                {{ msg }}
              </v-alert>
            </v-card-text>
          </v-card>
          <div v-else>
            <v-sheet rounded="lg" class="pa-4 mb-6 d-flex justify-space-between align-center" border>
              <div class="text-h6">
                Welcome, <strong class="purple-accent-1">{{ user }}</strong>
              </div>
              <v-btn color="red-lighten-1" @click="logout" variant="flat">
                Logout
              </v-btn>
            </v-sheet>
            <v-card class="pa-6 mb-6" rounded="lg" elevation="4">
              <div class="d-flex justify-space-between align-center mb-6">
                <h2 class="text-h5">Our Selection</h2>
                <v-btn
                    color="primary"
                    @click="loadProducts"
                    prepend-icon="mdi-refresh"
                    variant="elevated"
                >
                  Load Products
                </v-btn>
              </div>
              <v-row v-if="products.length">
                <v-col
                    v-for="p in products"
                    :key="p.id"
                    cols="12"
                    sm="6"
                    md="4"
                >
                  <v-card
                      @click="selectProduct(p)"
                      :variant="selected?.id === p.id ? 'tonal' : 'elevated'"
                      :color="selected?.id === p.id ? 'purple-accent-1' : undefined"
                      hover
                      rounded="lg"
                  >
                    <v-card-title class="text-subtitle-1 font-weight-bold">
                      {{ p.name }}
                    </v-card-title>
                    <v-chip v-if="selected?.id === p.id" class="ma-2" style="position: absolute; top: 4px; right: 4px;" color="white" text-color="black" size="small">Selected</v-chip>
                  </v-card>
                </v-col>
              </v-row>
              <v-alert v-else type="info" variant="tonal" border="start" icon="mdi-food-variant">
                Click "Load Products" to see our selection.
              </v-alert>
            </v-card>
            <v-card
                v-if="selected"
                class="pa-6"
                rounded="lg"
                elevation="4"
            >
              <h2 class="text-h5 mb-4">Order '{{ selected.name }}'</h2>

              <v-text-field
                  v-model="quantity"
                  type="number"
                  min="1"
                  label="Quantity"
                  variant="outlined"
                  density="comfortable"
                  class="mb-4"
              />
              <v-btn
                  color="green-lighten-1"
                  @click="order"
                  block
                  size="large"
                  prepend-icon="mdi-cart-check"
              >
                Submit Order
              </v-btn>
              <v-alert
                  v-if="msg"
                  class="mt-5"
                  type="success"
                  variant="tonal"
                  density="compact"
                  border="start"
              >
                {{ msg }}
              </v-alert>
            </v-card>
          </div>
        </v-col>
      </v-row>
    </v-container>
  </v-app>
</template>

<script lang="ts" setup>
import { ref, watch } from "vue";

interface Product {
  id: number;
  name: string;
}

const user = ref<string | null>(null);
const form = ref({ username: "", password: "" });
const msg = ref("");

const products = ref<Product[]>([]);
const selected = ref<Product | null>(null);
const quantity = ref(1);


watch(form, () => {
  msg.value = "";
}, { deep: true });

async function postEvent(eventType: string, capability: string, payload: any = {}) {
  try {
    await fetch("http://localhost:8080/api/messages", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        sender: user.value || "SnackShop",
        eventType,
        capability,
        payload: JSON.stringify(payload)
      })
    });
  } catch (error) {
    console.error("Failed to post event:", error);
    msg.value = "Network error. Could not connect to the server.";
  }
}

async function login() {
  try {
    const res = await fetch("http://localhost:8080/api/auth/login", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(form.value)
    });

    const json = await res.json();

    if (json.success) {
      user.value = json.username;
      msg.value = "Logged in successfully!";
      await postEvent("PROVIDES", "AuthCredentials", { username: user.value });
    } else {
      msg.value = json.message || "Login failed. Please check your credentials.";
    }
  } catch (e) {
    msg.value = "Login request failed. Is the server running?";
  }
}

async function registerUser() {
  try {
    const res = await fetch("http://localhost:8080/api/auth/register", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(form.value)
    });

    const json = await res.json();

    if (json.success) {
      user.value = json.username;
      msg.value = "Registered successfully! You are now logged in.";
      await postEvent("PROVIDES", "AuthCredentials", { username: user.value });
    } else {
      msg.value = json.message || "Registration failed.";
    }
  } catch (e) {
    msg.value = "Registration request failed. Is the server running?";
  }
}

function logout() {
  msg.value = `Goodbye, ${user.value}!`;
  user.value = null;
  products.value = [];
  selected.value = null;
  form.value = { username: "", password: "" };
  postEvent("REQUIRES", "AuthCredentials");
}

async function loadProducts() {
  await postEvent("REQUIRES", "ProductList");

  products.value = [
    { id: 1, name: "Crispy Chips" },
    { id: 2, name: "Sparkling Water" },
    { id: 3, name: "Chocolate Bar" },
    { id: 4, name: "Energy Drink" },
    { id: 5, name: "Gummy Bears" },
    { id: 6, name: "Iced Tea" },
  ];

  await postEvent("PROVIDES", "ProductList", products.value);
  msg.value = "";
}

function selectProduct(p: Product) {
  if (selected.value?.id === p.id) {
    selected.value = null;
  } else {
    selected.value = p;
    postEvent("PROVIDES", "ProductSelection", p);
  }
  msg.value = "";
}

function order() {
  msg.value = `Order for ${quantity.value}x '${selected.value?.name}' sent successfully!`;
  postEvent("PROVIDES", "OrderCreated", {
    product: selected.value,
    quantity: quantity.value
  });
  selected.value = null;
  quantity.value = 1;
}
</script>

<style scoped>
.v-card {
  position: relative;
}
</style>
