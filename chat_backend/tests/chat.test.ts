import { describe, beforeEach, expect, test } from "bun:test";
import { createInMemoryApp } from "../src/controllers/main";
import { password } from "bun";

describe("chat tests", () => {
  let app = createInMemoryApp();

  beforeEach(async () => {
    app = createInMemoryApp();
  });

  async function getToken(email = "test@test.com"): Promise<string> {
    await app.request("/api/v1/auth/register/", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        email: email,
        password: "password123",
        name: "Chat User",
      }),
    });

    const loginResponse = await app.request("/api/v1/auth/login", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        email: email,
        password: "password123",
      }),
    });
    const token = (await loginResponse.json()).token;
    return token;
  }

  async function createChat(token: string) {
    const createChatResponse = await app.request("/api/v1/chat/", {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        Authorization: `Bearer ${token}`,
      },
      body: JSON.stringify({ name: "Test Chat" }),
    });
    const response = await createChatResponse.json();
    const chatId = response.data.id;
    return chatId;
  }

  test("GET /chat/ - get user chats", async () => {
    const token = await getToken();
    const chatId = await createChat(token);
    const response = await app.request("/api/v1/chat/", {
      method: "GET",
      headers: { Authorization: `Bearer ${token}` },
    });
    expect(response.status).toBe(200);
    const responseData = await response.json();
    const data = responseData.data;
    expect(Array.isArray(data)).toBeTruthy();
    expect(data.length).toBe(1);
    expect(data[0].id).toBe(chatId);
  });
  test("GET /cht/ - get user chats wehn mulp")
});
