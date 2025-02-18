import { MongoClient, Db } from "mongodb";

class Database {
    private mongoUrl: string;
    private dbName: string;
    private client: MongoClient;
    private connected: Promise<Db>;

    constructor(mongoUrl: string, dbName: string) {
        this.mongoUrl = mongoUrl;
        this.dbName = dbName;
        this.client = new MongoClient(mongoUrl);

        this.connected = this.client.connect().then(client => {
            console.log(`[MongoClient] Connected to ${mongoUrl}/${dbName}`);
            return client.db(dbName);
        }).catch(err => {
            console.error("[MongoClient] Connection error:", err);
            throw err;
        });
    }

    public async getDb(): Promise<Db> {
        return this.connected;
    }

    public async status(): Promise<{ error: any | null; url?: string; db?: string }> {
        try {
            await this.connected;
            return { error: null, url: this.mongoUrl, db: this.dbName };
        } catch (err) {
            return { error: err };
        }
    }
}
export default Database;
