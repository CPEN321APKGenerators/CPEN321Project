import request from "supertest";
import app from "../../index"; // Adjust based on your app entry point
import { jest } from "@jest/globals";
import { JournalController } from "../../src/controllers/JournalController";
import { MongoClient, Db, Collection, Document, BulkWriteResult } from "mongodb";
import { client } from "../../services";
import fs from "fs";


/**
 * Test Suite: Journal API - Unmocked
 * - This test suite tests the **actual database and API endpoints** (no mocking).
 * - It validates various journal operations:
 *   - Creating, retrieving, updating, and deleting journal entries
 *   - Authentication and authorization failures
 *   - Edge cases like incorrect Google tokens and missing parameters
 */
describe("Journal API - Unmocked", () => {
    let unmocked_data_json: any = {}; // Default empty object
    // Attempt to load external test data if available
    try {
        const dataFilePath = `${__dirname}/../unmocked_data.json`; // Adjusted path

        if (fs.existsSync(dataFilePath)) {
            unmocked_data_json = require(dataFilePath);
        } else {
            console.log("Warning: unmocked_data.json not found. Using only environment variables.");
        }
    } catch (error) {
        console.log("Warning: Failed to load unmocked_data.json. Using only environment variables.", error);
    }
    const testGoogleToken = process.env.TEST_GOOGLE_TOKEN || unmocked_data_json.testGoogleToken;
    const google_num_id = process.env.GOOGLE_NUM_ID || unmocked_data_json.googleNumID;
    const dummy_token = "eyJhbGciOiJub25lIn0.eyJleHAiOjE4OTM0NTYwMDB9." // Expires in 2030

    const mockJournal = {
        date: "2025-03-11",
        userID: "llcce44@gmail.com",
        content: "Today was a good day.",
        googleNumID: google_num_id,
    };

    const mockJournal2 = {
        date: "2025-03-11",
        userID: "llcce44@gmail.com",
        content: "Today was a good day.",
        media: ["data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAOEAAADhCAMAAAAJbSJIAAABEVBMVEX///8AAAD/wgD/25L7+/ukpKTt7e1oaGi5ubn4+PiQkJD4vQCQbQD/xAD/35W5n2r41Y6Qe1Ph4eGwsLDGxsbV1dXy8vIiIiI0NDT/3JfR0dHn5+eCgoKJiYlhYWENDQ14eHi5jQBMTEw8PDxCQkIXFxctLS1WVlaWlpYLCwv/ygAWFhb/2oxwVQD/2ILfqgDRnwAoHwAhGQBbRQBKOADhwYH/1ngtIgCadQCpgQAnJydCMwC2iwBmZmb/02Wchln/0FT/yz41KADrswBrUQB+YADVogAVEAB4Z0SBb0o6MiGZeyXKrXRHPSn/yCfdvX5XSzJnWTwjHhSxmGUcFQBEOidWSjL/zEz/0F3k4NP/99HDaTteAAAMmUlEQVR4nO2deVvbOhaHwYlDAohyvQXibJCNpYWwtRCWFgKlvUs6d6YzdOb7f5BJCJaOvMq2FCIe//4jj2PrxZLO0TlHysJCpkyZMmXKlClTpkyZMmVKqLysYoGzzYqhq7JKNyqmHYZnGWq9vCi3ynXVsIL6plHtvnb7uKhbNXz7a6G28dpN46aNWsELqG2/drO4alvzAO6+dps4q+FC1F67QQJEIRbezhAkaoGxWGq+dmuEqFnChEVoBMv16rKsqlLmfFDEfXQJ8O1UNKskqyytsgMYl5x+WtzHn7U08mblVElrYZr9l5dYquGPNmTnm6hEps3alMfEnwx8PAEJVRjgLmk+f2BgZP2Vm8ZLRUxkPP+tO3+2PZ6OpNLa1Dsjw7AWtOqQTRZGWp4MRKtJAb8J4W7ZnLw0Yg2Lkd+URXggPlvEjFBGZYTyKyOUXxmh/MoI5VdGKL8yQvk1W0JLMyvajANAMyPMG812udPZ3+90yq0lfXYhhNkQWkVPvq6rW0yp9tSaBaGm+uaTGzvaLBjFExaKgcmsrhpaSsBHwgnN0FxWXfycLZgwX2z7oznqqHweFCyxhHk1umClKjg/IpZwJ5JPPKJQQtXF0uiUB4Nyp+FGFGocRRIWKY5yr6lWCqW8ZerVHm0+dkS+RYGEJnxXg6YOMErFKmVCRM6o4ggLPYBQL7pfkwGtyIbATJc4wh1Sd9So+Vj2gtohiM3UjwuUMMIKMISq7zjLF8FoNNI+L1DCCIGhCDTqoLKlnfZ5gRJFWAElAsFXqaTyQ9hkI4gwT0xhL+y6KttlaSSI0MYTadkMu66AC142Qq9LIUGEFfxulsOtOX7XuzvpnhgoMYSkk3YrEVdit6CZ6onBEkNo1Z37RPqceCT2BFl9MYQFbMwjazpwvW5X0GwqiBDPH5GWvOQYjIagtbAYQjzR1CO7Xh53aEFTjRhCUqUTuS7K16QkxFNpNca1y6keGSgxhNgpDfHYHGHCmpjoafYOkynGOFyQcxziQtx6ZFA7fy0lIbaHrWh76KwRd6WyhwUcwYj0aWznSrl8GgsvnmpRAxFPu9uClk+C1ha43a2oduNAxpKgVJug9SGp+VfDG06ixoKMhShCUiHfDfVMS+S6iIVkYgkiLJFQWzPsumV82Xa6BwZLVKzNwHtVgoOJY8+AhBOFbQ4QRVgiUbTgaC+IGm+kfF6whEWEDRIwbfhvGF8wQIGGuNyMMEKy7ltcLKt+VUI6iPsLi5aKzMxoAKDT9MyUWhUkZvZFBUsXhOYPiU0cq1WlGLVlqoRI5B4kkTlgOos/6O28vClNrdMlNkLT3ELz+N5SmsZgsO/5sCm0lk9sLUaT5XSGJbGFUWIJSzXvG/O8QcF7OQXXRFlq1AkNy6LLTUXXteWNnj/ZVC1deHmi+NpE2103BFRLZgcLcco2Z1Bfmteq/ny9SqJKIbvebm2zFxnNpkbYrg08fElnGHN6qx4r4szqvM3lXqtbLnc6nXK31asmri6xHWeQ9UiEmdbqa5PD0fSimaKMzSInk4RHD7Ak229RIkGB8UTMFPiQizCvUxW5rSLDnCoXYcXlP2wwmFOpCEn1jaNy9LpLKkK/Y+Qi8zkyEfrva2hGfEsiQt0XcNzy8PWzPIQgAvv47RtceIYvMKUhNMEg/LZ+9h0iXofZflkIbXDK2u9na2vrv8GO2gtZo0hCSPkyP9Zyudz6Z4jYDkaUhBBu3Xg3Acyt5f4FEfcDPTg5CCuA5Y/3uanW/oSIgS2WgtAGIA8rowOM+DtEDPDgZCC0wDR6coiU0UeM+BfsqDu+hlECQpiou+wjRYGI/4GIy36IEhCqZP/U0ZUyIQSIP/4BEBt+6YH5JyyCEM/qFBAinv0Nx+K1N3ww94QmSNKdOICKgm4cxHUK0bv3Zt4J7Tpp/RHCgGMRRMq92XA7qXNOaIFZ5ngIARXlF0b8DHc6umuU+BGWNNPknoMA8fKfWzSgonx5jxH/CTsqHankRqgttxplbzI7naCzdq64CZWnF9s/9uAoxKLvPVISmtPx0uLau00QWXtY8QASxFzu3SMci7CEhxMhrozpckzJw32290MfQAXdEg8OjsUOiN7wIawQm9XhhpgHcZnLr36AE8OI/fAfsKOCEnouhMUOvHlENSKzQKHDsWeWIa8RI55BJ3WxyZNQdR0hwOeQBB0kyM/3ggDHiLij0u6NE6BKT1jacZ+RwKW4wgDR34cQQMqD+0YhanwILRhfcN08hTQwy5z4TaO+iLR7c23yILT887v1lGVc8BcaLg/DAceIxIOjoje9SnpCkM6jFRb9YhAo+zsKnmV8EHN0gMpIS2j34P0uqZunQYTh7QuvL+OjX5uO7acCVF0jHSH9izurKxfwz05yDw76MqsRg9DRF4z4DrairOMCwgSEBpXNG/uNe1tUlVfSXL0NptEPjIBjD84hpANUnZbTpviE1Blex8+dCfXvIGKyciA4tk8jZxmiWxJmpGx/4neow1N0jq+mq1PUP4U3VROUVcLw9nE/1BK6EbHtp6I3SQnh2Stjr2pv+r9G6OsJ+Hw/ftVaXgc3vogDCBFp9yYRIVUdewf+1eiQQqzGRYSp+lXE3kfdiJR7k4CwBr962ocNQcMTz33ZBVP1/iumUI0OnCn17HsaQvo3oU5cswFSKMTtWH44sD/RvoyPkIPo8uDiEdrX8IsfPP9ptPcBXrAbY7oB0+hRPwHg+OHYSaXdmziEZh1+zy+6gJRVGFQoM/vhMPB0lQgQIObWqAAVO2GF8tT8XQ60cnEELoo+O2Iqg/gyj+fMpt6t0Q02jFSAqsdIaFBVO+cBsx1aufoELmMqz6JS9d6+nwjxHXBvWjYTITyfa/Ex2CtGyhZEZCnPgqn6yxSAComHj92bf5M2MBHqsBL2+CrE7UdK/ye4dhC5U9uCFijJNAofjsdijuTfmHop5ch82gpd1yA0hP15N+oACbhiYlgShmuEVxpreDXFMNOUKEfmsh/lcaAh5Yc3QwFhqt4nvB1XeL24hhdT0YRUNxrb+Wif0eXeXIcYRjpVnxpQQbn4hDYVkTlhmgrQ8AEaxuAzJKx23FtH6WNsQpuKyLBO5mhI2f6gI4eowFMyX8atp82YhCblqT0w/5vR8Bx+secf2lBJbIAl8MSi25iELkcmRj9CdPSm7efegFT9LodZ5lmjeIQGdWZ1PIcKKVfwywMvIkzV33MCjElowAr53SBPLRAR9SGiZwAEp+pnRZininOPruJFFqaI0INzHUEAo+aPXKbRZ8UYh3lq79ndVmxAxR2g2qVOiwxN1c+C0KIAT2MFvwAiFaCCv5AJUvUhjrxAwgKVWjoJSsVGIx7ewxvh/JsJsoQ8fBksVntoUxGZ+xQuPxpSoY2XABVMfCQIPIXoCxsh7cika8LYg4M32550VKZUfTLdMBHaVOblPmUnQiur8HZtm4q68pxlJsKR0zBCkypAeNhL2wSkUO5Nt6KDRE7M8HakWFZPcMW2+LjKoQVjxGNwz11YEMQZcMRASB3ccXfBpQ8hRAWoiKJS9bEVvQLWi9BTuwwPWLALoa2jRa/iJNHYhMvBAgmpX6DgBqh4UozP+sR5lhk/ZTOSEOryK0dfY+zenLofwNOXmeo2FuGnITeH/1l0/m2RPVUfQySayEB4yZdP8bg37Kl6ZuF6PhZCbktSIMq94T/LkAJpBsJHrt4wFnBvjhKuVkJ1wEx4nDwDFC604gSo4i+oo/X0npXw+EIQ4LN7MzGMR3w8Cde9yTwTQfh4JQxwgni4dbHF1Q45gq8wnLAv4vFESFkR8oARfIWhhIfczcRs9LTJRpioEmIeNDrIMRHyDSnMUjebbISJgobzoKdcjpFQ0lc4ev/GCdHHzTdOeJNz640R/vIAvjHCL+5B+NYIn3wA3YQWjkEnrZt7Rfm9wRzYndCcRNlLuKBEQGRBsH75A5Iy02lODydDZXPakHcWnXbSM1zYNt0YScLAV6/d5lii1xO+nfRl+4eJ68jde8PnWrcHAXxr67jC9GUXOxmIi3crsqyeRgE9dAJI6kudX0spkpzs3eGYcd415ns6cHtqL3hr62cEsIOz2mAN/PO8fzhcmW+Nbm821/119udnUHFGdkUUqRrZk4fV+dYfvwXp+9+wyHtAiktK/mf3yS7442GFqBO4ZVSL2rnz39dujgDRha3/O/RsP5dcDU9Zq+Z3UKi82vap2y3U3s5g3Kj57p7LG9Vu9JclUDf45HDLUOvl6DvMtcp1Nfxge9usTI4Yl1S6UTGZfhskL6tY4DJlypQpU6ZMmTJlypQpk5/+D48oniNdjlRLAAAAAElFTkSuQmCC"],
        googleNumID: google_num_id
    };
    const mockJournal_jpeg = {
        date: "2025-03-14",
        userID: "llcce44@gmail.com",
        content: "Today was a good day.",
        media: ["data:image/jpeg;base64,/9j/4AAQSkZJRgABAQAAAQABAAD/2wCEAAEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQ=="],
        googleNumID: google_num_id
    };
    const mockJournal_gif = {
        date: "2025-03-15",
        userID: "llcce44@gmail.com",
        content: "Today was a good day.",
        media: ["data:image/gif;base64,R0lGODlhAQABAPAAAAAAAAAAACH5BAEAAAAALAAAAAABAAEAAAICRAEAOw=="],
        googleNumID: google_num_id
    };
    const mockJournal_nobase65data = {
        date: "2025-03-15",
        userID: "llcce44@gmail.com",
        content: "Today was a good day.",
        media: ["R0lGODlhAQABAPAAAAAAAAAAACH5BAEAAAAALAAAAAABAAEAAAICRAEAOw=="],
        googleNumID: google_num_id
    };

    /**
     * Before running tests:
     * - Inserts test user data into the database.
     * - Ensures that test users exist to prevent errors during API calls.
     * - Uses `upsert: true` to avoid duplicates.
     */
    beforeAll(async () => {
        // console.log("\nCleaning up database before test...");
        // await client.db("cpen321journal").collection("journals").deleteMany({ userID: "llcce44@gmail.com" });
    
        // console.log("Database cleaned. Inserting user...");
    
        await client.db("cpen321journal").collection("users").updateOne(
            { userID: "llcce44@gmail.com" }, // Find existing user
            {
                $set: {
                    userID: "llcce44@gmail.com",
                    isPaid: false,
                    googleNumID: google_num_id
                }
            },
            { upsert: true } // Insert if not found
        );
        await client.db("cpen321journal").collection("users").updateOne(
            { userID: "testtest@gmail.com" }, // Find existing user
            {
                $set: {
                    userID: "testtest@gmail.com",
                    isPaid: false,
                    googleNumID: google_num_id
                }
            },
            { upsert: true } // Insert if not found
        );
        await client.db("cpen321journal").collection("users").updateOne(
            { userID: "testnogooglenumid@gmail.com" }, // Find existing user
            {
                $set: {
                    userID: "testtest@gmail.com",
                    isPaid: false
                }
            },
            { upsert: true } // Insert if not found
        );
        await client.db("cpen321journal").collection("users").updateOne(
            { userID: "testnojournal@gmail.com" }, // Find existing user
            {
                $set: {
                    userID: "testnojournal@gmail.com",
                    isPaid: false,
                    googleNumID: google_num_id
                }
            },
            { upsert: true } // Insert if not found
        );
    
        // 🔍 Check if the user was inserted
        const testUser = await client.db("cpen321journal").collection("users").findOne({ userID: "llcce44@gmail.com" });
        console.log("Inserted User After Cleanup:", testUser);
    
        // Wait a bit to make sure MongoDB processes the insert
        await new Promise(resolve => setTimeout(resolve, 500));
    });

    /**
     * Test Case: Unauthorized Access - GoogleNumID Mismatch
     * 
     * - **Inputs:**
     *   - Request: `GET /api/journal`
     *   - Headers: `Authorization: Bearer <valid_token>`
     *   - Query Parameters: `{ googleNumID: "123" }`
     * 
     * - **Expected Behavior:**
     *   - The API should reject the request due to GoogleNumID mismatch.
     *   - Response status code: **403** with message: `"Unauthorized: googleNumID does not match token"`.
     */
    it("should respond with googleID and Token not matched", async () => {
        const response = await request(app)
            .get("/api/journal")
            .set("Authorization", "Bearer " + testGoogleToken)
            .query({ date: mockJournal.date, userID: mockJournal.userID, googleNumID: "123" });

        expect(response.status).toBe(403);
        expect(response.body).toHaveProperty("message", "Unauthorized: googleNumID does not match token");
    });

    /**
     * Test Case: Authentication Failure - Invalid Google Token
     * 
     * - **Inputs:**
     *   - Request: `GET /api/journal`
     *   - Headers: `Authorization: Bearer <expired_token>`
     * 
     * - **Expected Behavior:**
     *   - The API should reject the request due to an invalid Google token.
     *   - Response status code: **403** with message: `"Invalid Google token when authenticating"`.
     */
    it("should respond with Invalid Google token when authenticating", async () => {
        const response = await request(app)
            .get("/api/journal")
            .set("Authorization", "Bearer " + "eyJhbGciOiJSUzI1NiIsImtpZCI6IjkxNGZiOWIwODcxODBiYzAzMDMyODQ1MDBjNWY1NDBjNmQ0ZjVlMmYiLCJ0eXAiOiJKV1QifQ.eyJpc3MiOiJodHRwczovL2FjY291bnRzLmdvb2dsZS5jb20iLCJhenAiOiI1ODUwNDAyMDQyMTAtMmE3ZW9hbjF1YnM3aGJjZWRyY24zb2xyZHJnN2dyMDAuYXBwcy5nb29nbGV1c2VyY29udGVudC5jb20iLCJhdWQiOiI1ODUwNDAyMDQyMTAtamxscW8ybjNvZHJmcmY4dGhiZ3ZoaXY2azFwYzVmMmcuYXBwcy5nb29nbGV1c2VyY29udGVudC5jb20iLCJzdWIiOiIxMDI3NjgzMjIyNzA1ODAzNzA2OTkiLCJlbWFpbCI6ImxsY2NlNDRAZ21haWwuY29tIiwiZW1haWxfdmVyaWZpZWQiOnRydWUsIm5hbWUiOiJDaHJpc3RpbmUgSklBTkciLCJwaWN0dXJlIjoiaHR0cHM6Ly9saDMuZ29vZ2xldXNlcmNvbnRlbnQuY29tL2EvQUNnOG9jSXVLSEZJZUpSVXZpeGV3Z2Z1UXRBZXRoUXNiMnV0VFY5MWNXbG1vcXdUWExQTGFnPXM5Ni1jIiwiZ2l2ZW5fbmFtZSI6IkNocmlzdGluZSIsImZhbWlseV9uYW1lIjoiSklBTkciLCJpYXQiOjE3NDE5OTEyMjcsImV4cCI6MTc0MTk5NDgyN30.kHuUUU9e3YXIlc2vRarTFfjOoDsvhAb1DoOaJBtX5I6_IH-z14enwUmCJ0Fhme7cDa8LkFQ0BR7-lbQH6503WDaZ33yhVXoMdELKOrrxWC-RrBaivJCbxptt-73glL-b2S_yf4SvECzpiB1PfRE0lNeGcfEL6mq6LyqZBQNHpx3G7x7j8n2AHNNCl3o2zq4jwPsBUW3ZkDrUuEgh4sPMOe3Ern5rjMqEkEQA7Nvc5mqVGaEnOVdaBGgqui2GvnDSHwu14SP4rQNWsbEqpDLIYByvr7YrKGebgq6uG-auoa7E-MSvhw6vV0GhMgzTdRZ8YufaYms2WdPoIj96WYh5Kg") // expired token
            .query({ date: mockJournal.date, userID: mockJournal.userID, googleNumID: google_num_id });

        expect(response.status).toBe(403);
        expect(response.body).toHaveProperty("message", "Invalid Google token when authenticating");
    });

    /**
     * Test Case: Authentication Failure - Missing Google Token
     * 
     * - **Inputs:**
     *   - Request: `GET /api/journal`
     *   - Headers: `Authorization: Bearer ""` (empty token)
     * 
     * - **Expected Behavior:**
     *   - The API should reject the request due to missing token.
     *   - Response status code: **400** with message: `"Missing googleToken"`.
     */
    it("should respond with missing google Token", async () => {
        const response = await request(app)
            .get("/api/journal")
            .set("Authorization", "Bearer " )
            .query({ date: mockJournal.date, userID: mockJournal.userID, googleNumID: google_num_id });

        expect(response.status).toBe(400);
        expect(response.body).toHaveProperty("message", "Missing googleToken");
    });

    /**
     * Test Case: Authentication Failure - Missing GoogleNumID
     * 
     * - **Inputs:**
     *   - Request: `GET /api/journal`
     *   - Headers: `Authorization: Bearer <valid_token>`
     *   - Query Parameters: `{ userID: "llcce44@gmail.com" }` (without `googleNumID`)
     * 
     * - **Expected Behavior:**
     *   - The API should reject the request due to missing GoogleNumID.
     *   - Response status code: **403** with message: `"Invalid Google token when authenticating"`.
     */
    it("should respond with missing googleNumID", async () => {
        const response = await request(app)
            .get("/api/journal")
            .set("Authorization", "Bearer " + "eyJhbGciOiJSUzI1NiIsImtpZCI6IjkxNGZiOWIwODcxODBiYzAzMDMyODQ1MDBjNWY1NDBjNmQ0ZjVlMmYiLCJ0eXAiOiJKV1QifQ.eyJpc3MiOiJodHRwczovL2FjY291bnRzLmdvb2dsZS5jb20iLCJhenAiOiI1ODUwNDAyMDQyMTAtMmE3ZW9hbjF1YnM3aGJjZWRyY24zb2xyZHJnN2dyMDAuYXBwcy5nb29nbGV1c2VyY29udGVudC5jb20iLCJhdWQiOiI1ODUwNDAyMDQyMTAtamxscW8ybjNvZHJmcmY4dGhiZ3ZoaXY2azFwYzVmMmcuYXBwcy5nb29nbGV1c2VyY29udGVudC5jb20iLCJzdWIiOiIxMDI3NjgzMjIyNzA1ODAzNzA2OTkiLCJlbWFpbCI6ImxsY2NlNDRAZ21haWwuY29tIiwiZW1haWxfdmVyaWZpZWQiOnRydWUsIm5hbWUiOiJDaHJpc3RpbmUgSklBTkciLCJwaWN0dXJlIjoiaHR0cHM6Ly9saDMuZ29vZ2xldXNlcmNvbnRlbnQuY29tL2EvQUNnOG9jSXVLSEZJZUpSVXZpeGV3Z2Z1UXRBZXRoUXNiMnV0VFY5MWNXbG1vcXdUWExQTGFnPXM5Ni1jIiwiZ2l2ZW5fbmFtZSI6IkNocmlzdGluZSIsImZhbWlseV9uYW1lIjoiSklBTkciLCJpYXQiOjE3NDE5OTEyMjcsImV4cCI6MTc0MTk5NDgyN30.kHuUUU9e3YXIlc2vRarTFfjOoDsvhAb1DoOaJBtX5I6_IH-z14enwUmCJ0Fhme7cDa8LkFQ0BR7-lbQH6503WDaZ33yhVXoMdELKOrrxWC-RrBaivJCbxptt-73glL-b2S_yf4SvECzpiB1PfRE0lNeGcfEL6mq6LyqZBQNHpx3G7x7j8n2AHNNCl3o2zq4jwPsBUW3ZkDrUuEgh4sPMOe3Ern5rjMqEkEQA7Nvc5mqVGaEnOVdaBGgqui2GvnDSHwu14SP4rQNWsbEqpDLIYByvr7YrKGebgq6uG-auoa7E-MSvhw6vV0GhMgzTdRZ8YufaYms2WdPoIj96WYh5Kg") // expired token
            .query({ date: mockJournal.date, userID: mockJournal.userID});

        expect(response.status).toBe(403);
        expect(response.body).toHaveProperty("message", "Invalid Google token when authenticating");
    });


    /**
     * Test Case: Create and Retrieve a Journal Entry
     * 
     * - **Inputs:**
     *   - Request: `POST /api/journal`
     *   - Headers: `Authorization: Bearer <valid_token>`
     *   - Body:
     *     ```json
     *     {
     *       "date": "2025-03-11",
     *       "userID": "llcce44@gmail.com",
     *       "content": "Today was a good day.",
     *       "googleNumID": "<valid_google_id>"
     *     }
     *     ```
     * - **Expected Behavior:**
     *   - The journal entry should be created successfully.
     *   - The `GET` request should retrieve the journal entry.
     *   - Response status codes: **200** for both `POST` and `GET`.
     */
    it("should create and retrieve a journal entry", async () => {
        const postResponse = await request(app)
            .post("/api/journal")
            .set("Authorization", "Bearer " + testGoogleToken)
            .send(mockJournal);
    
        expect(postResponse.status).toBe(200);
        expect(postResponse.body).toHaveProperty("message");
    
        // Wait for DB to update before retrieving
        await new Promise(resolve => setTimeout(resolve, 500)); 
    
        const getResponse = await request(app)
            .get("/api/journal")
            .set("Authorization", "Bearer " + testGoogleToken)
            .query({ date: mockJournal.date, userID: mockJournal.userID, googleNumID: google_num_id });
    
        expect(getResponse.status).toBe(200);
        expect(getResponse.body).toHaveProperty("journal");
    });

    /**
     * Test Case: User Not Found
     * 
     * - **Inputs:**
     *   - Request: `POST /api/journal`
     *   - Headers: `Authorization: Bearer <valid_token>`
     *   - Body:
     *     ```json
     *     {
     *       "date": "2025-03-11",
     *       "userID": "234@gmail.com",
     *       "googleNumID": "<valid_google_id>"
     *     }
     *     ```
     * 
     * - **Expected Behavior:**
     *   - The API should return a `404 Not Found` error.
     */
    it("should respond with user not found", async () => {
        const response = await request(app)
            .post("/api/journal")
            .set("Authorization", "Bearer " + testGoogleToken)
            .send({ 
                date: "2025-03-11",
                userID: "234@gmail.com", 
                googleNumID: google_num_id 
            });

        expect(response.status).toBe(404);
    });

    it("should create and retrieve a journal entry", async () => {
        const getResponse = await request(app)
            .get("/api/journal")
            .set("Authorization", "Bearer " + testGoogleToken)
            .query({ date: mockJournal.date, userID: "2434", googleNumID: google_num_id });
    
        expect(getResponse.status).toBe(404);
    });

    /**
     * Test Case: Update an Existing Journal Entry
     * 
     * - **Inputs:**
     *   - Request: `PUT /api/journal`
     *   - Headers: `Authorization: Bearer <valid_token>`
     *   - Body:
     *     ```json
     *     {
     *       "date": "2025-03-10",
     *       "userID": "llcce44@gmail.com",
     *       "googleNumID": "<valid_google_id>",
     *       "text": "Updated journal content"
     *     }
     *     ```
     * 
     * - **Expected Behavior:**
     *   - The journal entry should be updated successfully.
     *   - Response status code: **200**.
     */
    describe('PUT /api/journal', () => {
        it('should update an existing journal entry', async () => {
            // Setup initial entry
            const postResponse = await request(app)
            .post("/api/journal")
            .set("Authorization", "Bearer " + testGoogleToken)
            .send({
                date: "2025-03-10",
                userID: "llcce44@gmail.com",
                content: "Today was a good day.",
                googleNumID: google_num_id,
            });
    
            const putResponse = await request(app)
            .put("/api/journal")
            .set("Authorization", "Bearer " + testGoogleToken)
            .send({ date: "2025-03-10", userID: "llcce44@gmail.com", googleNumID: google_num_id, text: "help"});
            
            // expect(putResponse.status).toBe(200);
            console.log("put Journal: ", putResponse)
        });
      });

    
    /**
     * Test Case: Delete an Existing Journal Entry
     * 
     * - **Inputs:**
     *   - Request: `DELETE /api/journal`
     *   - Headers: `Authorization: Bearer <valid_token>`
     *   - Query Parameters:
     *     ```json
     *     {
     *       "date": "2025-03-13",
     *       "userID": "llcce44@gmail.com",
     *       "googleNumID": "<valid_google_id>"
     *     }
     *     ```
     * 
     * - **Expected Behavior:**
     *   - The journal entry should be deleted successfully.
     *   - Response status code: **200**.
     */
    describe('DELETE /api/journal', () => {
        it('should delete an existing journal entry', async () => {
            // Setup initial entry
            await client.db("cpen321journal").collection("journals").insertOne({
                date: "2025-03-13",
                userID: "llcce44@gmail.com",
                media: []
            });
        
            const response = await request(app)
            .delete("/api/journal")
            .set("Authorization", "Bearer " + testGoogleToken)
            .query({ date: "2025-03-13", userID: "llcce44@gmail.com", googleNumID: google_num_id});
        
            // Check response
            expect(response.status).toBe(200);
        });
    }); 

    /**
     * Test Group: getJournalFile (Download Journal Data)
     * 
     * - This section tests the `/api/journal/file` endpoint, which allows users to download journal entries in different formats.
     * - It verifies:
     *   - Successful downloads in different formats (CSV, PDF)
     *   - Error handling when journals do not exist
     *   - Invalid format requests
     */
    describe('getJournalFile', () => {
        /**
     * Test Case: Download Journal as CSV
     * 
     * - **Inputs:**
     *   - Request: `GET /api/journal/file`
     *   - Headers: `Authorization: Bearer <valid_token>`
     *   - Query Parameters:
     *     ```json
     *     {
     *       "userID": "llcce44@gmail.com",
     *       "format": "csv"
     *     }
     *     ```
     * 
     * - **Expected Behavior:**
     *   - The API should return a **200** status code.
     *   - The response should contain a **downloadURL** pointing to the CSV file.
     */
        it('should get a file (csv format)', async () => {
            // Setup initial entry
            const postResponse = await request(app)
            .post("/api/journal")
            .set("Authorization", "Bearer " + testGoogleToken)
            .send(mockJournal);
    
            expect(postResponse.status).toBe(200);
            expect(postResponse.body).toHaveProperty("message");
        
            const response = await request(app)
            .get("/api/journal/file")
            .set("Authorization", "Bearer " + testGoogleToken)
            .query({userID: "llcce44@gmail.com", format:"csv"})
        
            // Check response
            expect(response.status).toBe(200);
            expect(response.body).toHaveProperty("downloadURL");
        });

        /**
         * Test Case: Download Journal as PDF
         * 
         * - **Inputs:**
         *   - Request: `GET /api/journal/file`
         *   - Headers: `Authorization: Bearer <valid_token>`
         *   - Query Parameters:
         *     ```json
         *     {
         *       "userID": "llcce44@gmail.com",
         *       "format": "pdf"
         *     }
         *     ```
         * 
         * - **Expected Behavior:**
         *   - The API should return a **200** status code.
         *   - The response should contain a **downloadURL** pointing to the PDF file.
         */
        it('should get a file (pdf format)', async () => {
            // Setup initial entry
            const postResponse = await request(app)
            .post("/api/journal")
            .set("Authorization", "Bearer " + testGoogleToken)
            .send(mockJournal2);
    
            expect(postResponse.status).toBe(200);
            expect(postResponse.body).toHaveProperty("message");
        
            const response = await request(app)
            .get("/api/journal/file")
            .set("Authorization", "Bearer " + testGoogleToken)
            .query({userID: "llcce44@gmail.com", format:"pdf"})
        
            // Check response
            expect(response.status).toBe(200);
            expect(response.body).toHaveProperty("downloadURL");
            console.log("getJournalFile: ", response)
        });

        /**
         * Test Case: Download Failure - User Does Not Exist
         * 
         * - **Inputs:**
         *   - Request: `GET /api/journal/file`
         *   - Headers: `Authorization: Bearer <valid_token>`
         *   - Query Parameters:
         *     ```json
         *     {
         *       "userID": "testtestnonexist@gmail.com",
         *       "format": "csv"
         *     }
         *     ```
         * 
         * - **Expected Behavior:**
         *   - The API should return a **403 Forbidden** status code.
         *   - The response should contain an appropriate error message.
         */
        it('should not get a file (User does not exist)', async () => {
            const response = await request(app)
            .get("/api/journal/file")
            .set("Authorization", "Bearer " + testGoogleToken)
            .query({userID: "testtestnonexist@gmail.com", format:"csv"})
        
            // Check response
            expect(response.status).toBe(403);
            expect(response.body).toHaveProperty("message");
        });

        /**
         * Test Case: Invalid File Format
         * 
         * - **Inputs:**
         *   - Request: `GET /api/journal/file`
         *   - Headers: `Authorization: Bearer <valid_token>`
         *   - Query Parameters:
         *     ```json
         *     {
         *       "userID": "testtest@gmail.com",
         *       "format": "xslx"
         *     }
         *     ```
         * 
         * - **Expected Behavior:**
         *   - The API should return a **400 Bad Request** status code.
         *   - The response should indicate that the format is unsupported.
         */
        it('should not get a file (Invalid file format)', async () => {
            const response = await request(app)
            .get("/api/journal/file")
            .set("Authorization", "Bearer " + testGoogleToken)
            .query({userID: "testtest@gmail.com", format:"xslx"})
        
            // Check response
            expect(response.status).toBe(400);
        });

        it('should not a file (Invalid media format)', async () => {
            // Setup initial entry
            await client.db("cpen321journal").collection("journals").insertOne(
                {
                    userID: "testtest@gmail.com",
                    date: "2025-03-03",
                    media: [234]
                },
            );
        
            const response = await request(app)
            .get("/api/journal/file")
            .set("Authorization", "Bearer " + testGoogleToken)
            .query({userID: "llcce44@gmail.com", format:"csv"})
        
            // Check response
            expect(response.status).toBe(200);
            expect(response.body).toHaveProperty("downloadURL");
            console.log("getJournalFile: ", response)

            await client.db("cpen321journal").collection("journals").deleteOne(
                {
                    userID: "testtest@gmail.com",
                    date: "2025-03-03",
                    media: [234]
                },
            );
        });

        /**
         * Test Case: No Journals Found
         * 
         * - **Inputs:**
         *   - Request: `GET /api/journal/file`
         *   - Headers: `Authorization: Bearer <valid_token>`
         *   - Query Parameters:
         *     ```json
         *     {
         *       "userID": "testnojournal@gmail.com",
         *       "format": "csv"
         *     }
         *     ```
         * 
         * - **Expected Behavior:**
         *   - The API should return a **404 Not Found** status code.
         *   - The response should indicate that no journals exist for this user.
         */
        it('should not get a file (User has no journals)', async () => {
            const response = await request(app)
            .get("/api/journal/file")
            .set("Authorization", "Bearer " + testGoogleToken)
            .query({userID: "testnojournal@gmail.com", format:"csv"})
        
            // Check response
            expect(response.status).toBe(404);
        });

        /**
         * Test Case: Download Journal Containing JPEG
         * 
         * - **Inputs:**
         *   - Request: `GET /api/journal/file`
         *   - Headers: `Authorization: Bearer <valid_token>`
         *   - Query Parameters:
         *     ```json
         *     {
         *       "userID": "llcce44@gmail.com",
         *       "format": "pdf"
         *     }
         *     ```
         * 
         * - **Expected Behavior:**
         *   - The API should return a **200** status code.
         *   - The response should contain a **downloadURL** for a PDF with an embedded JPEG.
         */
        it('should get a file (JPEG journal entry)', async () => {
            // Setup initial entry
            const postResponse = await request(app)
            .post("/api/journal")
            .set("Authorization", "Bearer " + testGoogleToken)
            .send(mockJournal_jpeg);
    
            expect(postResponse.status).toBe(200);
            expect(postResponse.body).toHaveProperty("message");
        
            // this one has jpeg
            const response = await request(app)
            .get("/api/journal/file")
            .set("Authorization", "Bearer " + testGoogleToken)
            .query({userID: "llcce44@gmail.com", format:"pdf"})
        
            // Check response
            expect(response.status).toBe(200);
            expect(response.body).toHaveProperty("downloadURL");
            console.log("getJournalFile: ", response)
        });

        /**
         * Test Case: Download Journal Containing GIF
         * 
         * - **Inputs:**
         *   - Request: `GET /api/journal/file`
         *   - Headers: `Authorization: Bearer <valid_token>`
         *   - Query Parameters:
         *     ```json
         *     {
         *       "userID": "llcce44@gmail.com",
         *       "format": "pdf"
         *     }
         *     ```
         * 
         * - **Expected Behavior:**
         *   - The API should return a **200** status code.
         *   - The response should contain a **downloadURL** for a PDF with an embedded GIF.
         */
        it('should get a file (GIF journal entry)', async () => {
            // Setup initial entry
            const postResponse = await request(app)
            .post("/api/journal")
            .set("Authorization", "Bearer " + testGoogleToken)
            .send(mockJournal_gif);
    
            expect(postResponse.status).toBe(200);
            expect(postResponse.body).toHaveProperty("message");
        
            // this one has gif
            const response = await request(app)
            .get("/api/journal/file")
            .set("Authorization", "Bearer " + testGoogleToken)
            .query({userID: "llcce44@gmail.com", format:"pdf"})
        
            // Check response
            expect(response.status).toBe(200);
            expect(response.body).toHaveProperty("downloadURL");
            console.log("getJournalFile: ", response)
        });

        /**
         * Test Case: Download Journal with Non-Base64 Data
         * 
         * - **Inputs:**
         *   - Request: `GET /api/journal/file`
         *   - Headers: `Authorization: Bearer <valid_token>`
         *   - Query Parameters:
         *     ```json
         *     {
         *       "userID": "llcce44@gmail.com",
         *       "format": "pdf"
         *     }
         *     ```
         * 
         * - **Expected Behavior:**
         *   - The API should return a **200** status code.
         *   - The response should contain a **downloadURL** for a valid file.
         */
        it('should get a file (Non-base64 encoded data)', async () => {
            // Setup initial entry
            const postResponse = await request(app)
            .post("/api/journal")
            .set("Authorization", "Bearer " + testGoogleToken)
            .send(mockJournal_nobase65data);
    
            expect(postResponse.status).toBe(200);
            expect(postResponse.body).toHaveProperty("message");
        
            // this one has no base64 data
            const response = await request(app)
            .get("/api/journal/file")
            .set("Authorization", "Bearer " + testGoogleToken)
            .query({userID: "llcce44@gmail.com", format:"pdf"})
        
            // Check response
            expect(response.status).toBe(200);
            expect(response.body).toHaveProperty("downloadURL");
            console.log("getJournalFile: ", response)
        });
    }); 

});







