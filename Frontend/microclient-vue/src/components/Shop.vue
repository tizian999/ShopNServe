<template>
  <v-app theme="dark">
    <v-container class="py-8">
      <v-row justify="center">
        <v-col cols="12" md="10" lg="8">
          <header class="text-center mb-8">
            <h1 class="text-h3 font-weight-bold text-purple-accent-1">ShopNServe</h1>
            <p class="text-medium-emphasis">Your favorite snacks and drinks, delivered fast</p>
          </header>

          <v-card v-if="!user" class="pa-6 mx-auto" max-width="450" elevation="12" rounded="lg">
            <v-card-title class="text-h5 text-center mb-4">Welcome!</v-card-title>
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

              <v-btn class="mt-4" color="purple-accent-1" @click="login" block size="large">
                Login
              </v-btn>

              <v-btn class="mt-3" variant="tonal" @click="registerUser" block>
                Register
              </v-btn>

              <v-alert v-if="msg" class="mt-5" type="error" variant="tonal" density="compact" border="start">
                {{ msg }}
              </v-alert>
            </v-card-text>
          </v-card>

          <div v-else>
            <v-sheet rounded="lg" class="pa-4 mb-6 d-flex justify-space-between align-center" border>
              <div class="text-h6">
                Welcome, <strong class="purple-accent-1">{{ user }}</strong>
              </div>
              <v-btn color="red-lighten-1" @click="logout" variant="flat">Logout</v-btn>
            </v-sheet>

            <v-card class="pa-6 mb-6" rounded="lg" elevation="4">
              <div class="d-flex justify-space-between align-center mb-6">
                <h2 class="text-h5">Our Selection</h2>
                <v-btn color="primary" @click="loadProducts" prepend-icon="mdi-refresh" variant="elevated">
                  Load Products
                </v-btn>
              </div>

              <v-row v-if="products.length">
                <v-col v-for="p in products" :key="p.id" cols="12" sm="6" md="4">
                  <v-card
                      @click="selectProduct(p)"
                      :variant="selected?.id === p.id ? 'tonal' : 'elevated'"
                      :color="selected?.id === p.id ? 'purple-accent-1' : undefined"
                      hover
                      rounded="lg"
                  >
                    <v-card-title class="text-subtitle-1 font-weight-bold">{{ p.name }}</v-card-title>
                    <v-chip
                        v-if="selected?.id === p.id"
                        class="ma-2"
                        style="position:absolute; top:4px; right:4px;"
                        color="white"
                        text-color="black"
                        size="small"
                    >
                      Selected
                    </v-chip>
                  </v-card>
                </v-col>
              </v-row>

              <v-alert v-else type="info" variant="tonal" border="start" icon="mdi-food-variant">
                Click "Load Products" to see our selection.
              </v-alert>
            </v-card>

            <v-card v-if="selected" class="pa-6" rounded="lg" elevation="4">
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

              <v-btn color="green-lighten-1" @click="order" block size="large" prepend-icon="mdi-cart-check">
                Submit Order
              </v-btn>

              <v-alert v-if="msg" class="mt-5" type="error" variant="tonal" density="compact" border="start">
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

const MICROCLIENT = "Shop-Microclient";
const LOGIN_COMPONENT = "LoginView.vue";
const PRODUCTLIST_COMPONENT = "ProductListView.vue";

const user = ref<string | null>(null);
const form = ref({ username: "", password: "" });
const msg = ref("");

const products = ref<Product[]>([]);
const selected = ref<Product | null>(null);
const quantity = ref<number>(1);

watch(form, () => (msg.value = ""), { deep: true });

const JWT_KEY = "jwt";
function getJwt(): string | null {
  return localStorage.getItem(JWT_KEY);
}
function setJwt(token: string) {
  localStorage.setItem(JWT_KEY, token);
}
function clearJwt() {
  localStorage.removeItem(JWT_KEY);
}

function newTraceId(): string {
  // @ts-ignore
  if (typeof crypto !== "undefined" && crypto.randomUUID) return crypto.randomUUID();
  return `${Date.now()}-${Math.random().toString(16).slice(2)}`;
}

async function postBlackboardEvent(params: {
  traceId: string;
  senderComponent: string;
  capabilities: ("Authentication" | "ProductList" | "OrderPlaced" | "Authorization")[];
  payload: any;
}) {
  const jwt = getJwt();
  const isAuthentication = params.capabilities.includes("Authentication");

  const headers: Record<string, string> = { "Content-Type": "application/json" };

  if (!isAuthentication) {
    if (!jwt) {
      console.warn("[Blackboard] Missing JWT -> will likely 401");
    } else {
      headers["Authorization"] = `Bearer ${jwt}`;
    }
  }

  console.log("[Blackboard] POST /api/blackboard/messages", {
    traceId: params.traceId,
    caps: params.capabilities,
    isAuthentication,
    hasJwt: !!jwt,
    authHeaderSent: !!headers.Authorization,
  });

  const res = await fetch("/api/blackboard/messages", {
    method: "POST",
    headers,
    body: JSON.stringify({
      traceId: params.traceId,
      sender: { component: params.senderComponent, application: MICROCLIENT },
      capabilities: params.capabilities,
      payload: params.payload,
    }),
  });

  const json = await res.json().catch(() => ({}));
  return { status: res.status, json };
}

async function login() {
  msg.value = "";
  const tid = newTraceId();

  const { status, json } = await postBlackboardEvent({
    traceId: tid,
    senderComponent: LOGIN_COMPONENT,
    capabilities: ["Authentication"],
    payload: {
      action: "login",
      username: form.value.username,
      password: form.value.password,
    },
  });

  if (status === 401) {
    msg.value = "401 on Authentication. Backend treats it as non-auth request (capability mapping issue).";
    return;
  }

  if (json?.ok && json?.data?.token) {
    setJwt(json.data.token);
    user.value = json.data.username ?? form.value.username;
  } else {
    msg.value = json?.data?.error || "Login failed";
  }
}

async function registerUser() {
  msg.value = "";
  const tid = newTraceId();

  const { status, json } = await postBlackboardEvent({
    traceId: tid,
    senderComponent: LOGIN_COMPONENT,
    capabilities: ["Authentication"],
    payload: {
      action: "register",
      username: form.value.username,
      password: form.value.password,
    },
  });

  if (status === 401) {
    msg.value = "401 on Authentication. Backend treats it as non-auth request (capability mapping issue).";
    return;
  }

  if (json?.ok && json?.data?.token) {
    setJwt(json.data.token);
    user.value = json.data.username ?? form.value.username;
  } else {
    msg.value = json?.data?.error || "Registration failed";
  }
}

function logout() {
  msg.value = "";
  user.value = null;
  products.value = [];
  selected.value = null;
  form.value = { username: "", password: "" };
  clearJwt();
}

async function loadProducts() {
  msg.value = "";

  const tid = newTraceId();

  const { status, json } = await postBlackboardEvent({
    traceId: tid,
    senderComponent: PRODUCTLIST_COMPONENT,
    capabilities: ["ProductList"],
    payload: { action: "listProducts" },
  });

  if (status === 401) {
    msg.value = json?.data?.error || "Unauthorized (JWT missing/invalid)";
    return;
  }

  if (json?.ok && json?.data?.productList) {
    products.value = json.data.productList;
  } else {
    msg.value = json?.data?.error || "Failed to load products";
  }
}

function selectProduct(p: Product) {
  selected.value = selected.value?.id === p.id ? null : p;
  msg.value = "";
}

async function order() {
  msg.value = "";
  if (!selected.value) return;

  const tid = newTraceId();

  const { status, json } = await postBlackboardEvent({
    traceId: tid,
    senderComponent: PRODUCTLIST_COMPONENT,
    capabilities: ["OrderPlaced"],
    payload: { product: selected.value, quantity: quantity.value },
  });

  if (status === 401) {
    msg.value = json?.data?.error || "Unauthorized (JWT missing/invalid)";
    return;
  }

  if (json?.ok) {
    msg.value = `Order sent!`;
    selected.value = null;
    quantity.value = 1;
  } else {
    msg.value = json?.data?.error || "Order failed";
  }
}
</script>

<style scoped>
.v-card { position: relative; }
</style>