import { MongoClient } from "mongodb";

// export const client = new MongoClient(process.env.DB_URI ?? "mongodb://localhost:27017");
export const client: MongoClient = new MongoClient(
    process.env.DB_URI ?? "mongodb://localhost:27017"
  );
