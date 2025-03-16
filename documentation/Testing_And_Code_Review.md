# Example M5: Testing and Code Review

## 1. Change History

| **Change Date**   | **Modified Sections** | **Rationale** |
| ----------------- | --------------------- | ------------- |
| _Nothing to show_ |

---

## 2. Back-end Test Specification: APIs

### 2.1. Locations of Back-end Tests and Instructions to Run Them

#### 2.1.1. Tests

| **Interface**                 | **Describe Group Location, No Mocks**          | **Describe Group Location, With Mocks**      | **Mocked Components**                          |
|-------------------------------|-----------------------------------------------|-----------------------------------------------|-----------------------------------------------|
| **POST /api/journal**         |[`/Backend/tests/unmocked/journal.unmocked.test.ts#L243`](#)|[`/Backend/tests/mocked/journal.mocked.test.ts#L88`](#)| MongoDB, Google Auth Token Validation         |
| **GET /api/journal**          |[`/Backend/tests/unmocked/journal.unmocked.test.ts#L148`](#)|[`/Backend/tests/mocked/journal.mocked.test.ts#L137`](#)| MongoDB Query Operations                      |
| **PUT /api/journal**          |[`/Backend/tests/unmocked/journal.unmocked.test.ts#L324`](#)|[`/Backend/tests/mocked/journal.mocked.test.ts#L167`](#)| Database Update Operations                    |
| **DELETE /api/journal**       |[`/Backend/tests/unmocked/journal.unmocked.test.ts#L367`](#)|[`/Backend/tests/mocked/journal.mocked.test.ts#L198`](#)| MongoDB Query Operations                                       |
| **GET /api/journal/file**          |[`/Backend/tests/unmocked/journal.unmocked.test.ts#L395`](#)|[`/Backend/tests/mocked/journal.mocked.test.ts#L228`](#)| MongoDB Query Operations                   |
| **POST /api/payment-sheet**   |[`/Backend/tests/unmocked/payment.unmocked.test.ts#L57`](#)|[`/Backend/tests/mocked/payment.mocked.test.ts#L68`](#)| Stripe Customers, EphemeralKeys, PaymentIntents|
| **GET /api/analytics**        | -                                              | `analytics.mocked.test.ts`                    | MongoDB Analytics Queries                     |
| **POST /webhook**             |[`/Backend/tests/unmocked/webhook.unmocked.test.ts#L56`](#)|[`/Backend/tests/mocked/webhook.mocked.test.ts#L56`](#)| -                                             |
| **GET /api/profile**          |[`/Backend/tests/unmocked/user.unmocked.test.ts#L63`](#)|[`/Backend/tests/mocked/user.mocked.test.ts#L67`](#)| Firebase Admin, MongoDB User Collection       |
| **POST /api/profile**         |[`/Backend/tests/unmocked/user.unmocked.test.ts#L97`](#)|[`/Backend/tests/mocked/user.mocked.test.ts#L98`](#)| Google Auth API, MongoDB Insertions           |
| **GET /api/profile/isPaid**   |[`/Backend/tests/unmocked/user.unmocked.test.ts#L175`](#)|[`/Backend/tests/mocked/user.mocked.test.ts#L215`](#)| -                                             |
| **POST /api/profile/reminder**|[`/Backend/tests/unmocked/user.unmocked.test.ts#L206`](#)|[`/Backend/tests/mocked/user.mocked.test.ts#L177`](#)| MongoDB Update Operations                     |
| **POST /api/profile/fcmtoken**|[`/Backend/tests/unmocked/user.unmocked.test.ts#L246`](#)|[`/Backend/tests/mocked/user.mocked.test.ts#L139`](#)| Firebase Cloud Messaging (FCM)                |

#### 2.1.2. Commit Hash Where Tests Run

`[Insert Commit SHA here]`

#### 2.1.3. Explanation on How to Run the Tests

1. **Clone the Repository**:

  1. Open your terminal and run to clone:
     ```
     git clone https://github.com/example/your-project.git
     ```
  2. Set up environment variables by 
    export MONGODB_URI=`mongodb://mongo:27017`
    export STRIPE_SECRET=`your_stripe_accuont's_secret_key`
    export OPEN_API_KEY=`valid_open_ai_api_key`

    or create a new file called `.env` inside `/Backend` folder, put these variables in .env like this:
    ```
    DB_URI=mongodb://localhost:27017
    PORT=3001
    OPEN_API_KEY=valid_open_ai_api_key
    STRIPE_SECRET=your_stripe_accuont's_secret_key
    ```
  3.  Set up variables for testing by creating a file called `unmocked_data.json` inside `/Backend/tests` folder. Put these information in it like this:
  ```
  {
    "testGoogleToken": "this google token must be valid and corresponds to the google num id you put below",
    "googleNumID": "this field is a series of numbers of your google account numeric id",
    "OPEN_API_KEY": "valid_open_ai_api_key"
  }
  ```

  4. Change you working directory to `/Backend`. Run the command:
  ```
    npm test
  ```

2. **...**

### 2.2. GitHub Actions Configuration Location

`/.github/workflows/deploy.yml`

### 2.3. Jest Coverage Report Screenshots With Mocks

_(Placeholder for Jest coverage screenshot with mocks enabled)_



### 2.4. Jest Coverage Report Screenshots Without Mocks

_(Placeholder for Jest coverage screenshot without mocks)_

---

## 3. Back-end Test Specification: Tests of Non-Functional Requirements

### 3.1. Test Locations in Git

| **Non-Functional Requirement**  | **Location in Git**                              |
| ------------------------------- | ------------------------------------------------ |
| **Performance (Response Time)** | [`tests/nonfunctional/response_time.test.js`](#) |
| **Chat Data Security**          | [`tests/nonfunctional/chat_security.test.js`](#) |

### 3.2. Test Verification and Logs

- **Performance (Response Time)**

  - **Verification:** This test suite simulates multiple concurrent API calls using Jest along with a load-testing utility to mimic real-world user behavior. The focus is on key endpoints such as user login and study group search to ensure that each call completes within the target response time of 2 seconds under normal load. The test logs capture metrics such as average response time, maximum response time, and error rates. These logs are then analyzed to identify any performance bottlenecks, ensuring the system can handle expected traffic without degradation in user experience.
  - **Log Output**
    ```
    [Placeholder for response time test logs]
    ```

- **Chat Data Security**
  - **Verification:** ...
  - **Log Output**
    ```
    [Placeholder for chat security test logs]
    ```

---

## 4. Front-end Test Specification

### 4.1. Location in Git of Front-end Test Suite:

`frontend/src/androidTest/java/com/studygroupfinder/`

### 4.2. Tests

- **Use Case: Login**

  - **Expected Behaviors:**
    | **Scenario Steps** | **Test Case Steps** |
    | ------------------ | ------------------- |
    | 1. The user opens â€œAdd Todo Itemsâ€ screen. | Open â€œAdd Todo Itemsâ€ screen. |
    | 2. The app shows an input text field and an â€œAddâ€ button. The add button is disabled. | Check that the text field is present on screen.<br>Check that the button labelled â€œAddâ€ is present on screen.<br>Check that the â€œAddâ€ button is disabled. |
    | 3a. The user inputs an ill-formatted string. | Input â€œ_^_^^OQ#$â€ in the text field. |
    | 3a1. The app displays an error message prompting the user for the expected format. | Check that a dialog is opened with the text: â€œPlease use only alphanumeric charactersâ€. |
    | 3. The user inputs a new item for the list and the add button becomes enabled. | Input â€œbuy milkâ€ in the text field.<br>Check that the button labelled â€œaddâ€ is enabled. |
    | 4. The user presses the â€œAddâ€ button. | Click the button labelled â€œaddâ€. |
    | 5. The screen refreshes and the new item is at the bottom of the todo list. | Check that a text box with the text â€œbuy milkâ€ is present on screen.<br>Input â€œbuy chocolateâ€ in the text field.<br>Click the button labelled â€œaddâ€.<br>Check that two text boxes are present on the screen with â€œbuy milkâ€ on top and â€œbuy chocolateâ€ at the bottom. |
    | 5a. The list exceeds the maximum todo-list size. | Repeat steps 3 to 5 ten times.<br>Check that a dialog is opened with the text: â€œYou have too many items, try completing one firstâ€. |

  - **Test Logs:**
    ```
    [Placeholder for Espresso test execution logs]
    ```

- **Use Case: ...**

  - **Expected Behaviors:**

    | **Scenario Steps** | **Test Case Steps** |
    | ------------------ | ------------------- |
    | ...                | ...                 |

  - **Test Logs:**
    ```
    [Placeholder for Espresso test execution logs]
    ```

- **...**

---

## 5. Automated Code Review Results

### 5.1. Commit Hash Where Codacy Ran

`[Insert Commit SHA here]`

### 5.2. Unfixed Issues per Codacy Category

_(Placeholder for screenshots of Codacyâ€™s Category Breakdown table in Overview)_

### 5.3. Unfixed Issues per Codacy Code Pattern

_(Placeholder for screenshots of Codacyâ€™s Issues page)_

### 5.4. Justifications for Unfixed Issues

- **Code Pattern: [Usage of Deprecated Modules](#)**

  1. **Issue**

     - **Location in Git:** [`src/services/chatService.js#L31`](#)
     - **Justification:** ...

  2. ...

- ...


# Testing and Code Review Report

## 1. Change History

| **Change Date** | **Modified Sections** | **Rationale** |
| -------------- | --------------------- | ------------- |
| _Nothing to show_ |

---

## 2. Back-end Test Specification: APIs

### 2.1. Locations of Back-end Tests and Instructions to Run Them

#### 2.1.1. Tests

| **Interface**                 | **Describe Group Location, No Mocks** | **Describe Group Location, With Mocks** | **Mocked Components** |
| ----------------------------- | ------------------------------------- | --------------------------------------- | ---------------------- |
| **GET /name**                 | _To be filled_                        | _To be filled_                          | None                  |
| **POST /api/profile**         | _To be filled_                        | _To be filled_                          | None                  |
| **POST /storeFcmToken**       | _To be filled_                        | _To be filled_                          | None                  |
| **POST /changeReminder**      | _To be filled_                        | _To be filled_                          | None                  |
| **POST /api/journal**         | _To be filled_                        | _To be filled_                          | ['../services']       |
| **GET /userProfile**          | _To be filled_                        | _To be filled_                          | None                  |
| **POST /userProfile**         | _To be filled_                        | _To be filled_                          | None                  |
| **GET /api/profile**          | _To be filled_                        | _To be filled_                          | None                  |
| **POST /api/profile/fcmtoken**| _To be filled_                        | _To be filled_                          | None                  |
| **GET /api/journal**          | _To be filled_                        | _To be filled_                          | None                  |
| **GET /api/journal/file**     | _To be filled_                        | _To be filled_                          | None                  |
| **PUT /api/journal**          | _To be filled_                        | _To be filled_                          | None                  |
| **POST /api/profile/reminder**| _To be filled_                        | _To be filled_                          | None                  |
| **POST /api/payment**         | _To be filled_                        | _To be filled_                          | None                  |
| **DELETE /api/journal**       | _To be filled_                        | _To be filled_                          | None                  |
| **POST /webhook**             | _To be filled_                        | _To be filled_                          | ['../services']       |
| **GET /api/profile/isPaid**   | _To be filled_                        | _To be filled_                          | None                  |

#### 2.1.2. Commit Hash Where Tests Run

`[Insert Commit SHA here]`

#### 2.1.3. Explanation on How to Run the Tests

1. **Clone the Repository**:
   ```sh
   git clone https://github.com/example/your-project.git
   cd your-project
   ```
2. **Install Dependencies**:
   ```sh
   npm install
   ```
3. **Run Tests**:
   ```sh
   npm test
   ```

### 2.2. GitHub Actions Configuration Location

`~/.github/workflows/backend-tests.yml`

### 2.3. Jest Coverage Report Screenshots With Mocks

_(Placeholder for Jest coverage screenshot with mocks enabled)_

### 2.4. Jest Coverage Report Screenshots Without Mocks

_(Placeholder for Jest coverage screenshot without mocks)_

---

## 3. Back-end Test Specification: Tests of Non-Functional Requirements

### 3.1. Test Locations in Git

| **Non-Functional Requirement**  | **Location in Git** |
| ------------------------------- | ------------------- |
| **Performance (Response Time)** | _To be filled_     |
| **Security Testing**            | _To be filled_     |

### 3.2. Test Verification and Logs

- **Performance (Response Time)**
  - **Verification:** This test simulates concurrent API calls to measure system response times under normal load. The goal is to ensure requests are handled within the expected threshold.
  - **Log Output:**
    ```
    [Placeholder for response time test logs]
    ```

- **Security Testing**
  - **Verification:** Ensures sensitive user data remains encrypted and cannot be accessed without proper authorization.
  - **Log Output:**
    ```
    [Placeholder for security test logs]
    ```

---

## 4. Front-end Test Specification

### 4.1. Location in Git of Front-end Test Suite:

`frontend/tests/`

### 4.2. Tests

- **Use Case: Login**
  - **Expected Behaviors:**
    | **Scenario Steps** | **Test Case Steps** |
    | ------------------ | ------------------- |
    | 1. User enters credentials | Input valid email and password |
    | 2. Clicks Login | Check login request and response |
    | 3. Successful login | Redirect to dashboard |
  - **Test Logs:**
    ```
    [Placeholder for login test logs]
    ```

---

## 5. Automated Code Review Results

### 5.1. Commit Hash Where Code Review Ran

`[Insert Commit SHA here]`

### 5.2. Unfixed Issues per Code Review Tool

_(Placeholder for screenshots of Code Review report)_

### 5.3. Justifications for Unfixed Issues

- **Issue 1: [Example Issue]**
  - **Location in Git:** _To be filled_
  - **Justification:** _To be filled_

---

## Next Steps
- Integrate test results into the report.
- Add Jest coverage reports.
- Complete the code review section based on automated tool results.

---

This document will be converted to PDF and pushed into the **documentation** folder in GitHub.

