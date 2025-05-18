# Original UI Sequence Diagram (Member Registration)

This document contains a sequence diagram illustrating the member registration workflow through the web interface (`index.xhtml`) of the Kitchensink application.

## Member Registration Sequence

```plantuml
@startuml
actor User
participant "JSF View (index.xhtml)" as View
participant "MemberController" as Controller
participant "MemberRegistration (EJB)" as RegistrationService
participant "EntityManager (JPA)" as EM
participant "CDI Events" as Events
participant "MemberListProducer" as ListProducer
participant "MemberRepository" as Repository

User -> View: 1. Navigates to page / Fills form
View -> Controller: 2. User submits form (invokes `register()` action)
Controller -> RegistrationService: 3. register(newMember)
RegistrationService -> EM: 4. persist(newMember)
RegistrationService -> Events: 5. fire(newMember Event)

group Asynchronous Event Processing
  Events -> ListProducer: 6. (Delivers event) onMemberListChanged(member)
  ListProducer -> Repository: 7. findAllOrderedByName()
  Repository -> EM: 8. query for all members
  EM ->> Repository: 9. returns members
  Repository ->> ListProducer: 10. returns members
  ListProducer -> ListProducer: 11. Updates internal 'members' list
end

RegistrationService ->> Controller: 12. (Returns from register call)
Controller -> View: 13. Adds FacesMessage (e.g., "Registered!")
Controller -> Controller: 14. initNewMember() (resets form bean)
Controller ->> View: 15. (JSF lifecycle continues, navigation occurs)

View -> ListProducer: 16. getMembers() (for re-rendering table)
ListProducer ->> View: 17. Returns updated member list

View -> User: 18. Displays success message and re-rendered page with new member in list
@enduml
```

**Explanation of Participants:**

*   **User**: The end-user interacting with the web browser.
*   **JSF View (`index.xhtml`)**: The web page rendered in the browser, handles user input and displays data.
*   **`MemberController`**: CDI managed bean (JSF backing bean) that handles UI logic for registration.
*   **`MemberRegistration` (EJB)**: Stateless EJB that handles the business logic of persisting a new member.
*   **`EntityManager` (JPA)**: JPA component responsible for database interactions (persisting, querying).
*   **CDI Events**: Represents the CDI event bus used for decoupled communication.
*   **`MemberListProducer`**: CDI bean that produces the list of members for the UI and observes events to refresh this list.
*   **`MemberRepository`**: CDI bean responsible for data access operations related to members.

This diagram shows the flow from the user initiating a registration, through the backend processing including database interaction and event handling, and finally the UI update reflecting the new registration. 