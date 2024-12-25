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
});
