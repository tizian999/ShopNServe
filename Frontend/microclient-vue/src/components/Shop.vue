<template>
  <v-app theme="dark">
    <v-container fluid class="py-6 px-6">
      <header class="d-flex align-center justify-space-between mb-6">
        <div>
          <h1 class="text-h3 font-weight-bold text-purple-accent-1">
            ShopNServe
          </h1>
          <p class="text-medium-emphasis">
            Your favorite snacks and drinks, delivered fast
          </p>
        </div>
        <div v-if="user" class="d-flex align-center ga-3">
          <v-btn
              variant="tonal"
              prepend-icon="mdi-history"
              @click="loadOrders"
          >
            Orders
          </v-btn>
          <div class="text-medium-emphasis">
            Logged in as <strong>{{ user }}</strong>
          </div>
          <v-btn
              color="red-lighten-1"
              variant="flat"
              @click="logout"
          >
            Logout
          </v-btn>
        </div>
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
              class="mb-3"
          />
          <v-text-field
              v-model="form.password"
              type="password"
              label="Password"
              prepend-inner-icon="mdi-lock-outline"
              variant="outlined"
          />
          <v-btn
              class="mt-4"
              color="purple-accent-1"
              block
              size="large"
              @click="login"
          >
            Login
          </v-btn>
          <v-btn
              class="mt-3"
              variant="tonal"
              block
              @click="registerUser"
          >
            Register
          </v-btn>
          <v-alert
              v-if="msg"
              class="mt-5"
              type="error"
              variant="tonal"
              density="compact"
              border="start"
          >
            {{ msg }}
          </v-alert>
        </v-card-text>
      </v-card>
      <div v-else>
        <div class="layout">
          <!-- PRODUCTS -->
          <v-card class="pa-5 flex-grow-1" rounded="lg" elevation="4">

            <div class="d-flex justify-space-between align-center mb-4">
              <h2 class="text-h5">Products</h2>

              <div class="d-flex ga-2">
                <v-btn
                    color="primary"
                    prepend-icon="mdi-refresh"
                    variant="elevated"
                    @click="loadProducts"
                >
                  Load
                </v-btn>
                <v-btn
                    variant="tonal"
                    prepend-icon="mdi-cart-off"
                    @click="clearCart"
                >
                  Clear Cart
                </v-btn>
              </div>
            </div>
            <!-- AUTO RESPONSIVE GRID -->
            <div
                v-if="products.length"
                class="product-grid"
            >
              <v-card
                  v-for="p in products"
                  :key="p.id"
                  class="product-card"
                  rounded="lg"
                  hover
                  :variant="inCart(p.id) ? 'tonal' : 'elevated'"
                  :color="inCart(p.id) ? 'purple-darken-2' : undefined"
                  @click="toggleCart(p)"
              >
                <v-card-title class="d-flex justify-space-between">
                  <span class="text-truncate">
                    {{ p.name }}
                  </span>
                  <span class="text-medium-emphasis">
                    {{ priceText(p) }}
                  </span>
                </v-card-title>
              </v-card>
            </div>
            <v-alert
                v-else
                type="info"
                variant="tonal"
                border="start"
            >
              Click "Load" to see our selection.
            </v-alert>
          </v-card>
          <!-- ORDER PANEL -->
          <v-card class="pa-5 order-card" rounded="lg" elevation="10">

            <div class="d-flex justify-space-between mb-3">
              <h2 class="text-h6">Order</h2>
              <div class="text-medium-emphasis">
                Items: {{ items.length }}
              </div>
            </div>
            <v-divider class="mb-4" />
            <div v-if="!items.length" class="text-medium-emphasis">
              Select products to add them to your cart.
            </div>
            <div v-else class="d-flex flex-column ga-3">

              <v-card
                  v-for="it in items"
                  :key="it.product.id"
                  variant="outlined"
                  rounded="lg"
                  class="pa-3"
              >
                <div class="d-flex justify-space-between align-center">

                  <div class="flex-grow-1">
                    <div class="font-weight-bold">
                      {{ it.product.name }}
                    </div>

                    <div class="text-medium-emphasis">
                      {{ priceText(it.product) }}
                      · Line:
                      {{ fmt(it.product.price_cents * it.qty) }}
                    </div>
                  </div>
                  <!-- COMPACT QUANTITY -->
                  <v-text-field
                      v-model.number="it.qty"
                      type="number"
                      min="1"
                      density="compact"
                      variant="outlined"
                      hide-details
                      class="qty-field"
                  />

                </div>
              </v-card>
              <v-divider />
              <div class="d-flex justify-space-between">
                <div>Total</div>
                <div class="font-weight-bold">
                  {{ totalText }}
                </div>
              </div>
              <v-btn
                  color="green-lighten-1"
                  prepend-icon="mdi-cart-check"
                  block
                  size="large"
                  @click="submitOrder"
              >
                Submit Order
              </v-btn>
            </div>
          </v-card>

        </div>
        <!-- ORDER HISTORY -->
        <v-card
            v-if="orders.length"
            class="mt-8 pa-6"
            rounded="lg"
            elevation="6"
        >
          <h2 class="text-h6 mb-4">Order History</h2>
          <v-expansion-panels>
            <v-expansion-panel
                v-for="o in orders"
                :key="o.id"
            >
              <v-expansion-panel-title>
                Order #{{ o.id }} —
                {{ fmt(o.total_cents) }}
              </v-expansion-panel-title>
              <v-expansion-panel-text>

                <div class="text-medium-emphasis mb-2">
                  {{ o.created_at }}
                </div>
                <div class="d-flex flex-wrap ga-2">
                  <v-chip
                      v-for="it in parseItems(o.items)"
                      :key="it.product.id"
                      color="purple-darken-2"
                      variant="tonal"
                  >
                    {{ it.product.name }}
                    × {{ it.quantity }}
                    —
                    {{ fmt(it.product.price_cents * it.quantity) }}
                  </v-chip>
                </div>
              </v-expansion-panel-text>
            </v-expansion-panel>
          </v-expansion-panels>
        </v-card>
      </div>
    </v-container>
  </v-app>
</template>

<script lang="ts" setup>
import { computed, reactive, ref } from "vue";

type Cap = "Authentication" | "ProductList" | "OrderPlaced" | "Authorization";
type Product = { id: number; name: string; price_cents?: number; stock?: number };
type CartItem = { product: Product; qty: number };

const MICROCLIENT = "Shop-Microclient";
const LOGIN_COMPONENT = "LoginView.vue";
const PRODUCTLIST_COMPONENT = "ProductListView.vue";

const user = ref<string | null>(null);
const form = reactive({ username: "", password: "" });
const msg = ref("");

const products = ref<Product[]>([]);
const cart = reactive<Record<number, CartItem>>({});
const orders = ref<any[]>([]);

const items = computed(() => Object.values(cart));
const clamp = (n: any) => Math.max(1, Math.trunc(Number(n) || 1));
const inCart = (id: number) => !!cart[id];
const remove = (id: number) => delete cart[id];
const clearCart = () => Object.keys(cart).forEach((k) => delete cart[Number(k)]);

const hasPrice = (p: Product) => Number.isFinite(p.price_cents);
const fmt = (cents: number) => `${(cents / 100).toFixed(2).replace(".", ",")} €`;
const priceText = (p: Product) => (hasPrice(p) ? fmt(p.price_cents!) : "—");

const totalCents = computed(() =>
    items.value.reduce((s, it) => (hasPrice(it.product) ? s + it.product.price_cents! * clamp(it.qty) : s), 0)
);
const totalText = computed(() => (items.value.every((it) => hasPrice(it.product)) ? fmt(totalCents.value) : "—"));

const JWT_KEY = "jwt";
const SESSION_KEY = "sessionId";
const getJwt = () => localStorage.getItem(JWT_KEY);
const setJwt = (t: string) => localStorage.setItem(JWT_KEY, t);
const clearJwt = () => localStorage.removeItem(JWT_KEY);
const getSessionId = () => localStorage.getItem(SESSION_KEY);
const setSessionId = (id: string) => localStorage.setItem(SESSION_KEY, id);
const clearSessionId = () => localStorage.removeItem(SESSION_KEY);

const newTraceId = () =>
    // @ts-ignore
    (typeof crypto !== "undefined" && crypto.randomUUID) ? crypto.randomUUID() : `${Date.now()}-${Math.random().toString(16).slice(2)}`;
const getOrCreateSessionId = () => getSessionId() || (setSessionId(newTraceId()), getSessionId()!);

const norm = (raw: any): Product => {
  const id = Math.trunc(Number(raw?.id) || 0);
  const name = String(raw?.name ?? "");
  const pc = raw?.price_cents ?? raw?.priceCents;
  const price_cents = Number.isFinite(Number(pc)) ? Math.trunc(Number(pc)) : undefined;
  return { id, name, price_cents, stock: Number(raw?.stock ?? 0) || 0 };
};

function toggleCart(p: Product) {
  msg.value = "";
  cart[p.id] ? delete cart[p.id] : (cart[p.id] = { product: p, qty: 1 });
}

function parseItems(json: string) {
  try {
    return JSON.parse(json);
  } catch {
    return [];
  }
}

async function postBlackboardEvent(params: { traceId: string; senderComponent: string; capabilities: Cap[]; payload: any }) {
  const jwt = getJwt();
  const isAuth = params.capabilities.includes("Authentication");
  const headers: Record<string, string> = { "Content-Type": "application/json" };
  if (!isAuth && jwt) headers["Authorization"] = `Bearer ${jwt}`;

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
  setSessionId(tid);

  const { status, json } = await postBlackboardEvent({
    traceId: tid,
    senderComponent: LOGIN_COMPONENT,
    capabilities: ["Authentication"],
    payload: { action: "login", username: form.username, password: form.password },
  });

  if (status === 401) return (msg.value = "401 on Authentication.");
  if (json?.ok && json?.data?.token) {
    setJwt(json.data.token);
    user.value = json.data.username ?? form.username;
  } else msg.value = json?.data?.error || "Login failed";
}

async function registerUser() {
  msg.value = "";
  const tid = newTraceId();
  setSessionId(tid);

  const { status, json } = await postBlackboardEvent({
    traceId: tid,
    senderComponent: LOGIN_COMPONENT,
    capabilities: ["Authentication"],
    payload: { action: "register", username: form.username, password: form.password },
  });

  if (status === 401) return (msg.value = "401 on Authentication.");
  if (json?.ok && json?.data?.token) {
    setJwt(json.data.token);
    user.value = json.data.username ?? form.username;
  } else msg.value = json?.data?.error || "Registration failed";
}

function logout() {
  msg.value = "";
  user.value = null;
  products.value = [];
  clearCart();
  form.username = "";
  form.password = "";
  clearJwt();
  clearSessionId();
}

async function loadProducts() {
  msg.value = "";
  const tid = getOrCreateSessionId();

  const { status, json } = await postBlackboardEvent({
    traceId: tid,
    senderComponent: PRODUCTLIST_COMPONENT,
    capabilities: ["ProductList"],
    payload: { action: "listProducts" },
  });

  if (status === 401) {
    clearJwt(); clearSessionId(); user.value = null;
    return (msg.value = json?.data?.error || "Unauthorized");
  }

  const list = json?.data?.productList ?? [];
  products.value = Array.isArray(list) ? list.map(norm) : [];
}

async function submitOrder() {
  msg.value = "";
  if (!items.value.length) return;

  const tid = getOrCreateSessionId();
  const payload = {
    items: items.value.map((it) => ({
      product: { id: it.product.id, name: it.product.name, price_cents: it.product.price_cents },
      quantity: clamp(it.qty),
    })),
    total_cents: items.value.every((it) => hasPrice(it.product)) ? totalCents.value : null,
  };

  const { status, json } = await postBlackboardEvent({
    traceId: tid,
    senderComponent: PRODUCTLIST_COMPONENT,
    capabilities: ["OrderPlaced"],
    payload,
  });

  if (status === 401) {
    clearJwt(); clearSessionId(); user.value = null;
    return (msg.value = json?.data?.error || "Unauthorized");
  }

  if (json?.ok) { msg.value = "Order sent!"; clearCart(); }
  else msg.value = json?.data?.error || "Order failed";
}
async function loadOrders() {
  msg.value = "";
  const tid = getOrCreateSessionId();

  const { status, json } = await postBlackboardEvent({
    traceId: tid,
    senderComponent: PRODUCTLIST_COMPONENT,
    capabilities: ["OrderHistory"],
    payload: {action: "listOrders"},
  });

  if (status === 401) {
    clearJwt();
    clearSessionId();
    user.value = null;
    return (msg.value = json?.data?.error || "Unauthorized");
  }

  orders.value = json?.data?.orders || [];
}
</script>

<style scoped>
.layout {
 display: flex;
 gap: 24px;
 align-items: flex-start;
}

.product-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 16px;
}

.product-card {
  cursor: pointer;
  transition: transform 0.15s ease, box-shadow 0.15s ease;
}

.product-card:hover {
  transform: translateY(-2px);
}

.order-card {
  width: 340px;
  flex: 0 0 340px;
  position: sticky;
  top: 16px;
}

.qty-field {
  width: 64px;
}

.qty-field :deep(input) {
  text-align: center;
  padding: 4px 6px !important;
}
</style>