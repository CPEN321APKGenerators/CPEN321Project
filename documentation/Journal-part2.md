## Journal - Part 2

#### 1. Contribution of each member
Contribution of each team member: describe the work done by each team member towards the
completion of this milestone (1-2 sentences per member). Specify the time (in hours) spent by each
team member towards completion of the milestone.

Amod Ghimire - Implementation and testing of Frontend of the Application
Kevin Li - Implementation and testing of Complex Algorithm 
Nyi Nyi Oo - Implementation and Testing of Chatbot
Christine Jiang - Implementation and Testing of Backend API and Frontend, Github Action configuration


#### 2. Github Repository Info
Link to your GitHub repository: [CPEN 321 Journal Project](https://github.com/CPEN321APKGenerators/CPEN321Project)
Commit SHA of your final released version: (on the main branch)



#### 3. Location of front-end and back-end tests
Front-End tests: `Frontend/app/src/androidTest/java/com/example/cpen321project`
Back-end tests: 
|Type of Tests | Location |
| -------- | ------- |
| **Performance (Response Time)**   | [`/Backend/tests/*`](https://github.com/CPEN321APKGenerators/CPEN321Project/tree/main/Backend/tests) (All tests for api endpoints)    |
| **Encryption of Entries** | [`/Backend/tests/unmocked/journal.unmocked.test.ts#L266`](https://github.com/CPEN321APKGenerators/CPEN321Project/blob/main/Backend/tests/unmocked/journal.unmocked.test.ts)       |
| **Usability (Frontend)**    | [`Frontend/app/src/androidTest/java/com/example/cpen321project/Nonfunctional_clicks_test.kt`](https://github.com/CPEN321APKGenerators/CPEN321Project/blob/main/Frontend/app/src/androidTest/java/com/example/cpen321project/Nonfunctional_clicks_test.kt)    |
| **General Frontend tests**    | [`Frontend/app/src/androidTest/java/com/example/cpen321project`](https://github.com/CPEN321APKGenerators/CPEN321Project/tree/main/Frontend/app/src/androidTest/java/com/example/cpen321project)    |


#### 4. Information about Physical Device tested
Manufacturer: Samsung
Model: S10


#### 5. Information about Back-end Server
Public IP of Back-end server: ec2-35-183-201-213.ca-central-1.compute.amazonaws.com
Domain name: https://cpen321project-journal.duckdns.org
Rasa Action Server (NodeJSWrapper): https://54.234.28.190:3001/api/chat
RASA Action Server (EC2): https://ec2-54-234-28-190.compute-1.amazonaws.com:5055/webhook



#### 6. Neccessary User Accounts to run our App
To run our app, the following accounts and associated credentials or tokens are required:

- Firebase Account:
    - Used for authentication and managing user data (e.g., profile info, FCM tokens).
    - Required: Firebase Admin SDK JSON file (cpen321project-c324e-firebase-adminsdk.json).

- Stripe Account:
    - Used for handling payment processing when users upgrade to the Premium account.
    - Required: Stripe Secret Key and Publishable Key (cpen321project-stripe-secret.txt).

- Google Account:
    - Used for user login via OAuth.
    - Required: Google OAuth credentials including Web Client ID, Google Numeric ID. An updated google token is only required if you are testing this app.

- OpenAI Account:
    - Used for processing journal content through the GPT-based AI therapy bot.
    - Required: Valid OpenAI API Key (OPEN_API_KEY).

- Server Secret:
    - Required for encrypting FCM tokens and ensuring secure operations on the backend.
    - File: severSecret.txt (must be requested from the team).


#### 7. 'Complex' Use Cases
Our core complexity is implemented in the weekly trend analysis feature within the sentiment analytics system:

##### Use Case: Display Sentiment Trends in Analytics

- How It Works:
    - When the user accesses the analytics dashboard, we fetch their activity and emotion logs stored over the past week.

    - These logs include weights and metadata produced via the OpenAI LLM and saved during journal entry submission.

    - The analysis pipeline:
        - Parses emotion and activity scores from the last 7 days.

        - Computes trends by evaluating gradients (rise or fall) in data over time.

        - Applies filtering logic to ignore minor fluctuations (defined dynamically based on average activity frequency).

        - Matches emotion and activity trends chronologically, using a sliding time window to infer correlations.

        - Categorizes these as trend patterns like ++, --, +-, -+.

- Why This is Complex:
    - Implementing meaningful gradient detection requires nuanced tuning of thresholds.

    - Cross-matching multiple trends in sequence with noise filtering and categorization is non-trivial.

    - We had to build and maintain a data structure that dynamically classifies and tracks trend types and outputs summaries.

    - The system balances performance with insightfulness, which required iterations of optimization and testing.




#### 8. Humblebrag
a description of any technical extra part of your project you are particularly proud of.
TODO: Everyone should write one thing we are proud of!!

One of our proudest technical achievements is our secure-by-design encryption system, which integrates several layers of protection:

- AES Encryption of Journal Entries
Implemented via the crypto_functions.ts module, all journal data is encrypted before being stored in MongoDB. We verified this with test cases that confirm the database only stores encrypted content, and that decryption only occurs server-side for authenticated users.

- Google-based Token Authentication
All sensitive operations (create/edit/delete journals, fetch profile, etc.) require a valid Google token. These are validated on the backend (authentication_functions.ts), ensuring access control without needing to manage passwords.

- FCM Token Encryption
Users' FCM tokens (for notifications) are encrypted using a server-stored secret before saving to the database. This protects against misuse of tokens and safeguards users from spam notification attacks.


#### 9. Limitations of Our Project
A list of limitations of your project.

While we’re proud of our app, it has a few limitations:

- No Offline Support
    - The app requires an internet connection for nearly all actions: login, journaling, analytics, etc.

- Image-Only Media Support:
    - Users can only attach static images (JPG, PNG, JPEG) to their entries. Support for audio or video journaling is not yet implemented.

- No Undo for Deleted Entries:
    - Once a journal entry is deleted, recovery is not possible.

- Sentiment Analysis Sensitivity
    - The accuracy of our emotional trend analysis depends on how descriptive the user’s text is. Sparse entries may lead to weak insights.

- Token Expiry for Tests
    - Google tokens used for testing expire quickly (~1–2 hours), making automation and CI/CD pipelines harder to maintain.

- Single Language
    - The app is primarily designed with English prompts and UI. the underlying NLP (via OpenAI) is capable of handling multiple languages, the app's reflection prompts from the chatbot and the interface does not support languages other than English.

#### 10. Reflections on Generative AI Technologies
Reflections on using Generative AI technologies for software engineering tasks. You must answer
and elaborate on all the following questions in 1-2 paragraphs each:
    a. Which Generative AI technologies did you use in the scope of this course?
    b. For which phases of the overall Software Engineering process and which concrete tasks
    in these phases was Generative AI the most helpful and why? Discuss phases such as
    requirements elicitation and documentation, design, implementation, code review, testing,
    and project refinement (for the final release).
    c. For which phases of the overall Software Engineering process and which concrete tasks
    in these phases was Generative AI the least helpful and why? Discuss phases such as
    requirements elicitation and documentation, design, implementation, code review, testing,
    and project refinement (for the final release).
    d. Is there anything else you would like to share about AI’s impact on your software
    engineering tasks?
