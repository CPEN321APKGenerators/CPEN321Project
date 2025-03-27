import axios from 'axios';
import { TextEncoder, TextDecoder } from 'util';
import fs from "fs";
import path from "path";

const serverSecret = fs.readFileSync(path.join(__dirname, '../config/serverSecret.txt'), 'utf8').trim()||process.env.SERVER_SECRET;
console.log(serverSecret)

// Derive AES-CBC Key using PBKDF2
export async function deriveKey(googleNumID: string) {
    const encoder = new TextEncoder();
    const salt = encoder.encode(serverSecret);
    const baseKey = await crypto.subtle.importKey(
        "raw",
        encoder.encode(googleNumID),
        { name: "PBKDF2" },
        false,
        ["deriveKey"]
    );

    return await crypto.subtle.deriveKey(
        {
            name: "PBKDF2",
            salt,
            iterations: 100000,
            hash: "SHA-256",
        },
        baseKey,
        { name: "AES-CBC", length: 256 },
        false,
        ["encrypt", "decrypt"]
    );
}

// Encrypt Text or Base64 Media
export async function encryptData(data: string, key: CryptoKey) {
    const encoder = new TextEncoder();
    const dataBuffer = encoder.encode(data);

    const iv = crypto.getRandomValues(new Uint8Array(16));

    const encryptedData = await crypto.subtle.encrypt(
        { name: "AES-CBC", iv },
        key,
        dataBuffer
    );

    const combinedData = new Uint8Array(iv.byteLength + encryptedData.byteLength);
    combinedData.set(iv, 0);
    combinedData.set(new Uint8Array(encryptedData), iv.byteLength);

    return btoa(String.fromCharCode(...combinedData));
}

// Decrypt Text or Base64 Media
export async function decryptData(encryptedText: string, key: CryptoKey) {
    const decoder = new TextDecoder();
    const encryptedData = Uint8Array.from(atob(encryptedText), c => c.charCodeAt(0));

    const iv = encryptedData.slice(0, 16);
    const data = encryptedData.slice(16);

    const decryptedData = await crypto.subtle.decrypt(
        { name: "AES-CBC", iv },
        key,
        data
    );

    return decoder.decode(decryptedData);
}
