# M3 - Requirements and Design

## 1. Change History
<!-- Leave blank for M3 -->

## 2. Project Description



## 3. Requirements Specification
### **3.1. Use-Case Diagram**
- : Authentication With Google or Facebook; -A

- : Create, Delete, Edit and Export journal entries and analytics. -N

- : Ability to set realistic journaling habits and provide reminders.

- : Voice input functionality for people with disabilities or if User prefer this. -C

- : Adding photos and videos of events to the entries. -A

- : Symptom Tracking for physical and mental health to share with healthcare providers. -A

- : Play music (stress relief, frequency music, copyright free though) in the background according to user's current mood -C

- : Privacy, Manage how data is stored; Ability to delete user data;  -N

- : Reporting Bugs or Issues To Admin; -N

- : Suggest activities according to user's mood; -C

### **3.2. Actors Description**
1. **Users**: 
2. **Admin**: ...


### **3.3. Functional Requirements**
<a name="fr1"></a>

1. **[WRITE_FUNCTIONAL_REQUIREMENT_1_NAME_HERE]** 
    - **Overview**:
        1. [WRITE_FUNCTIONAL_REQUIREMENT_1_1_NAME_HERE]
        2. ...
    
    - **Detailed Flow for Each Independent Scenario**: 
        1. **[WRITE_FUNCTIONAL_REQUIREMENT_1_1_NAME_HERE]**:
            - **Description**: ...
            - **Primary actor(s)**: ... 
            - **Main success scenario**:
                1. ...
                2. ...
            - **Failure scenario(s)**:
                - 1a. ...
                    - 1a1. ...
                    - 1a2. ...
                - 1b. ...
                    - 1b1. ...
                    - 1b2. ...
                - 2a. ...
                    - 2a1. ...
                    - 2a2. ...

        2. ...
    
2. **[WRITE_FUNCTIONAL_REQUIREMENT_1_NAME_HERE]** 
    - **Overview**:
        1. [WRITE_FUNCTIONAL_REQUIREMENT_1_1_NAME_HERE]
        2. ...
    
    - **Detailed Flow for Each Independent Scenario**: 
        1. **[WRITE_FUNCTIONAL_REQUIREMENT_1_1_NAME_HERE]**:
            - **Description**: ...
            - **Primary actor(s)**: ... 
            - **Main success scenario**:
                1. ...
                2. ...
            - **Failure scenario(s)**:
                - 1a. ...
                    - 1a1. ...
                    - 1a2. ...
                - 1b. ...
                    - 1b1. ...
                    - 1b2. ...
                - 2a. ...
                    - 2a1. ...
                    - 2a2. ...

        2. ...
3. **[WRITE_FUNCTIONAL_REQUIREMENT_1_NAME_HERE]** 
    - **Overview**:
        1. [WRITE_FUNCTIONAL_REQUIREMENT_1_1_NAME_HERE]
        2. ...
    
    - **Detailed Flow for Each Independent Scenario**: 
        1. **[WRITE_FUNCTIONAL_REQUIREMENT_1_1_NAME_HERE]**:
            - **Description**: ...
            - **Primary actor(s)**: ... 
            - **Main success scenario**:
                1. ...
                2. ...
            - **Failure scenario(s)**:
                - 1a. ...
                    - 1a1. ...
                    - 1a2. ...
                - 1b. ...
                    - 1b1. ...
                    - 1b2. ...
                - 2a. ...
                    - 2a1. ...
                    - 2a2. ...

        2. ...
4. **Speech To Text** 
    - **Overview**:
        1. Set Language: The actor selects a Language for Voice Input
        2. Speech to Text conversion: The actor use speech to text conversion to record journal entries and communicate to the assistant through voice.
    - **Detailed Flow for Each Independent Scenario**:
        1. **Set Language**:
            - **Description**: Select a language from a list of supported languages for voice input
            - **Primary actor(s)**: User
            - **Main success scenario**:
                1. The user presses the gear icon to access the settings menu.
                2. The user chooses their Preferred Speech Recognition Language from a drop down menu.
                3. The user exits the settings menu
                4. System updates the language model for speech-to-text conversion.
            - **Failure scenario(s)**:
                - 2a. The list of supported language is not displayed due to network errors
                    - 2a1. An error message is displayed telling user the error, and potential solutions
                    - 2a2. System prompts the user to try again.
                - 4a. System cannot update the language model due to network errors
                    - 4a1. An error message is displayed telling user the error, and potential solutions
                    - 4a2. System prompts the user to try again.
        2. **Speech to Text Conversion**:
            - **Description**: Converts speech to text in a journal entry message
            - **Primary actor(s)**: User
            - **Main success scenario**:
                1. The user presses the voice typing button beside the text input box.
                2. System starts recording.
                3. The user talk for a while, presses the voice typing button again to end the recording
                4. System converts the user's speech to text according to the selected language (Default is English), display it in the text input box.
                5. The user reviews the speech to text conversion, and can edit it if neccessary.
            - **Failure scenario(s)**:
                - 2a. No Microphone Permission
                    - 1a1. System shows a permission rationale and convince the user to enable microphone permissions for voice recording.
                    - 1a2. The app prompots the user for microphone permission for retry.
                - 3a. Poor or No Audio Input
                    - 1b1. The current volume of the user's voice is displayed
                    - 1b2. System prompts the user to speak again.
                - 4a. Speech Recognition Failure
                    - 2a1. An error message is displayed for speech recognition failure, and suggest alternative input methods
                    - 2a2. System prompts the user to check the voice input language setting.
    
4. **Play Mood Music in the Background** 
    - **Overview**:
        1. Select and Play Music: The system selects and plays appropriate music based on detected mood from Mood Tracking functionality.
        2. User Control Over Music: The user can play, pause, or change the background music.
    
    - **Detailed Flow for Each Independent Scenario**: 
        1. . **Select and Play Music**:
            - **Description**: Based on the mood tracking statistics, system selects and plays appropriate music.
            - **Primary actor(s)**: System
            - **Main success scenario**:
                1. System retrieves mood tracking data.
                2. System selects a matching music track from a list of copyright-free music.
                3. Music starts playing in the background.
            - **Failure scenario(s)**:
                - 1a. System fails to retrieve mood tracking data.
                    - 1a1. An error message is displayed for retrieving mood tracking data
                    - 1a2. System assumes the mood is peaceful.
                - 2a. No Matching Music Available
                    - 2a1. The error is logged.
                    - 2a2. System selects a default relaxing music
                - 2b. Cannot select a matching music due to network error
                    - 2b1. The error is logged.
                    - 2b2. System selects a default relaxing music
                - 3a. Music playback error
                    - 3a1. An error message is displayed for music playback error, and suggest alternative input methods
        2. **User Control Over Music**:
            - **Description**: The user can play, pause, or change to another background music.
            - **Primary actor(s)**: User 
            - **Main success scenario**:
                1. User accesses the music control panel.
                2. User presses the play, pause, or change the music button.
                3. System executes the command.
            - **Failure scenario(s)**:
                - 2a. Cannot display the list of music available
                    - 2a1. An error message is displayed for failure of retrieving music list, and suggest alternative input methods
                    - 2a2. System prompts the user to try again.
                - 3a. Music player fails to respond
                    - 3a1. An error message is displayed
                    - 3a2. System suggests the user to restart the app.
5. **Acitivity Suggestions** 
    - **Overview**:
        1. : The system shows some suggested activities based on detected mood from Mood Tracking functionality.
        
    - **Detailed Flow for Each Independent Scenario**: 
        1. . **Select and Play Music**:
            - **Description**: Based on the mood tracking statistics, system selects and plays appropriate music.
            - **Primary actor(s)**: System
            - **Main success scenario**:
                1. System retrieves mood tracking data.
                2. System compose a prompt based on user's mood tracking data, and invoke an LLM
                3. Suggested activities are displayed to the user.
            - **Failure scenario(s)**:
                - 1a. System fails to retrieve mood tracking data.
                    - 1a1. An error message is displayed for retrieving mood tracking data
                    - 1a2. System assumes the mood is peaceful.
                - 2a. Failure to invoke LLM
                    - 2a1. An error message is logged for invoke LLM failure
                    - 2a2. System selects a suggests some general relaxing exercise
                - 3a. Activity description fails to load
                    - 3a1. An error message is displayed telling user the error, and potential solutions
                    - 2a2. System prompts the user to try again.

### **3.4. Screen Mockups**


### **3.5. Non-Functional Requirements**
<a name="nfr1"></a>

1. **[WRITE_NAME_HERE]**
    - **Description**: ...
    - **Justification**: ...
2. ...


## 4. Designs Specification
### **4.1. Main Components**
1. **[WRITE_NAME_HERE]**
    - **Purpose**: ...
    - **Interfaces**: 
        1. ...
            - **Purpose**: ...
        2. ...
2. ...


### **4.2. Databases**
1. **[WRITE_NAME_HERE]**
    - **Purpose**: ...
2. ...


### **4.3. External Modules**
1. **[WRITE_NAME_HERE]** 
    - **Purpose**: ...
2. ...


### **4.4. Frameworks**
1. **[WRITE_NAME_HERE]**
    - **Purpose**: ...
    - **Reason**: ...
2. ...


### **4.5. Dependencies Diagram**


### **4.6. Functional Requirements Sequence Diagram**
1. [**[WRITE_NAME_HERE]**](#fr1)\
[SEQUENCE_DIAGRAM_HERE]
2. ...


### **4.7. Non-Functional Requirements Design**
1. [**[WRITE_NAME_HERE]**](#nfr1)
    - **Validation**: ...
2. ...


### **4.8. Main Project Complexity Design**
**[WRITE_NAME_HERE]**
- **Description**: ...
- **Why complex?**: ...
- **Design**:
    - **Input**: ...
    - **Output**: ...
    - **Main computational logic**: ...
    - **Pseudo-code**: ...
        ```
        
        ```


## 5. Contributions
- ...
- ...
- ...
- ...
