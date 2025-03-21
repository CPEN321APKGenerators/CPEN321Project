## 1. Manual code review
a. Code is well documented, good variable names, efficient, no bad design smells,
correct error handling: 10/10
- Explanation of the point: There are many API calls to be made in the front end that is TODO. The settings activity is empty and doesn't have any plan on it. What does tvLoggedinuser mean? Why not just loggedinuser? Name is good can make it better.
- Authentication: Good variable names (signInButton, device, ...), catches and logs authentication faliures, and uses hashed nounce. Improvement would be adding documentation and removing the hardcoded WEB_CLIENT_ID.
- Users: All API endpoints are well-documented, with clean implementations.
Overal, the deployment guide for setting up frontend, backend and firebase is clear. The code directories are well-structured. And variable names are easy to understand, with error handling logic.

## 2. Manual test review
a. Tests are complete (all APIs exposed to the frontend are tested, three main use cases are tested), errors and edge cases are thoroughly tested, correct assertions are used:
9/10
Frontend:
- Explanation of the point: The front end has some redundant tests. The Join group and create group test looks like they are testing the same thing. The front end testing use cases includes login, create/join, and swipe. It may be tested but the testing document is just the same as the original starting document
- UI tests exist, but no network failure handling.

Backend:
- Authentication: Tests cover base cases for sigin in, could use tests for user creation, authentication faliures, and invalid sign-in responses.
- Users: All user API endpoints are tested, with success and failing cases.

b. Tests implementation matches the requirements and design: 9/10
- Explanation of the point: The swipe is only tested for left and right however it's not tested with restaurants unavailability as mentioned in your requirement doc
- Most tests implementation matches the requirements and design in the API.md file. 

c. Tests are well-structured: x/10
- Explanation of the point: 

- Authentication: Tests are well strucutred and uses proper syntax.

- User: Tests are named with their testing api name and indicated whether they are mocked or unmocked tests. They are also organized into directories.


d. Tests are comprehensive (good coverage): x/10
- Explanation of the point: Core backend functionality are tested. However, some backend files are left uncovered and unjustified.

- Authentication: Tests cover the login and nav to homepage, missing faliure scenario test cases. 

- Users: Some routes for the user component are not covered, and the uncovered parts are not justified.


e. Non-functional requirements are tested well: 5/10
- Explanation of the point: 

5/10 Didn't test two of the non-functional requirements - the usability and response time.


f. All backend tests can be run automatically: x/10
10/10
- Explanation of the point: Mocked and unmocked tests are set up correctly on Github actions. They even have unit tests which are not required.


## 3. Automated code review
a. Codacy runs with the required setup: 10/10
- Explanation of the point: The codacy runs in the codacy dashboard since there are issues shown in the files.

b. All remaining Codacy issues are well-justified: 5/10
- Explanation of the point: There are no codacy issues that are justified.

## 4. Fault: report one major implementation issue you found in your peer-team app. 
The report should contain the details about the issue (with screenshots) and the severity of the issue
- Issues that can be easily found with automated tools, like Codacy, ChatGPT, etc.
are not considered major. Look for deep “semantic” issues that require human
intelligence.
- Yes, you will find some major issues – there is no app without issues a few weeks
before the final deadline
- If you report that you cannot find any major fault and then a TA does, you will lose
marks. Otherwise, you will be given the full mark
- Explanation of the point: They say radius is required in the backend in the SessionRoutes.ts (Line 66). However, in the frontend they don't check that if it is empty in CreateGroupPage.kt (Line 118).
Moderategrouppage.kt (Line 137) API call is missing an error handler.
- Users' firebase cloud message tokens are easily obtainable using GET - your users are exposed to potential spam attacks like receiving unauthorized notifications.

- or: user experience issues:
    - Use more icons rather than text
    - larger font size, larger button size so users can click easier
