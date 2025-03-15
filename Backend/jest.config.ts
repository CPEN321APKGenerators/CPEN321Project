import type { JestConfigWithTsJest } from 'ts-jest';

const config: JestConfigWithTsJest = {
  preset: 'ts-jest',
  testEnvironment: 'node',
  setupFilesAfterEnv: ['./tests/setup.ts'],
  testTimeout: 1000000,
  collectCoverageFrom: ['src/**/*.ts', 'index.ts'],
};

export default config;
