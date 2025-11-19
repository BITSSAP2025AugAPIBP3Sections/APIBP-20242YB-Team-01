import axios, { AxiosInstance } from "axios";
import CircuitBreaker from "opossum";
import { CONFIG } from "../config";

const axiosClient: AxiosInstance = axios.create({
  baseURL: CONFIG.gatewayBaseUrl,
  timeout: 15000,
  headers: {
    Authorization: CONFIG.gatewayApiKey ? `Bearer ${CONFIG.gatewayApiKey}` : undefined,
    "Content-Type": "application/json",
  },
});

// wrapper that does a POST
async function doPost(path: string, body: any) {
  const res = await axiosClient.post(path, body);
  return res.data;
}

// circuit breaker options
const options = {
  timeout: 16000,
  errorThresholdPercentage: 50,
  resetTimeout: 10_000, // 10 sec
};

const breaker = new CircuitBreaker(doPost, options);

breaker.on("open", () => console.warn("🔴 Gateway circuit breaker OPEN"));
breaker.on("halfOpen", () => console.warn("🟡 Gateway circuit breaker HALF-OPEN"));
breaker.on("close", () => console.warn("🟢 Gateway circuit breaker CLOSED"));

export async function postToGateway(path: string, body: any) {
  // returns gateway response or throws
  return breaker.fire(path, body);
}
