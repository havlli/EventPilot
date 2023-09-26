[![logo_png_url]][repo_url]
---
[![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![codecov](https://codecov.io/gh/havlli/EventPilot/graph/badge.svg?token=T39ORJEZSP)](https://codecov.io/gh/havlli/EventPilot)
[![CodeFactor](https://www.codefactor.io/repository/github/havlli/eventpilot/badge)](https://www.codefactor.io/repository/github/havlli/eventpilot)
[![Test Coverage](https://github.com/havlli/EventPilot/actions/workflows/test-coverage.yml/badge.svg)](https://github.com/havlli/EventPilot/actions/workflows/test-coverage.yml)
[![Build & Publish OCI Image](https://github.com/havlli/EventPilot/actions/workflows/docker-publish.yml/badge.svg)](https://github.com/havlli/EventPilot/actions/workflows/docker-publish.yml)

EventPilot is a Discord bot written in Java using the Spring Boot, Project Reactor and Discord4J library. Its primary functionality is to help users create and manage events, allowing other Discord members to sign up for these events. Whether you're organizing gaming sessions, study groups, or any other kind of event, EventPilot simplifies the process by providing a user-friendly interface within your Discord server.

## Table of Contents

- [Features](#features)
- [About Project](#about-project)
    - [Technologies Used](#technologies-used)
    - [Lessons Learned](#lessons-learned)
- [Getting Started](#getting-started)
    - [Prerequisites](#prerequisites)
    - [Run Application Locally](#run-application-locally-in-docker)
    - [Setup Development Environment](#setup-development-environment)
- [Usage](#usage)
    - [Commands](#commands)
- [Contributing](#contributing)
- [FAQ](#faq)
- [License](#license)
## Features

- Create and manage events within your Discord server.
- Allow members to sign up for events with a simple command.
- Display event details, including the organizer, date, time, and available slots.
- Automatic event reminders to keep participants informed.

## About Project
  This project began as a personal hobby, driven by my interest in reactive programming and experimentation with the Reactor library. Initially, it was intended for personal use within my Discord server among friends. However, over time, it evolved into a robust application with its own database, asynchronous operations, and a CI pipeline.

### Technologies Used
  _The project leverages the following technologies:_
- [**Java 17**](https://www.oracle.com/java/technologies/downloads/) - The programming language used to develop the bot.
- [**Project Reactor**](https://projectreactor.io) - Library for reactive programming in Java, utilized to handle asynchronous operations.
- [**Discord4J**](https://discord4j.com) - Discord API wrapper for Java, enabling seamless integration with the Discord platform.
- [**Spring Boot**](https://spring.io/projects/spring-boot) - Framework used for building Java applications, providing features such as web development, data persistence, and logging.
- [**PostgreSQL**](https://www.postgresql.org) - An open-source relational database management system, used to store and manage event-related data.
- [**Flyway**](https://flywaydb.org) - Database migration tool that ensures the consistency and integrity of the database schema.
- [**JUnit 5**](https://junit.org/junit5/) - Testing framework for Java, utilized for writing unit tests.
- [**Mockito**](https://site.mockito.org) - Mocking framework used for testing, providing flexibility in creating and verifying mock objects.
- [**Testcontainers**](https://testcontainers.com) - A Java library that simplifies the use of Docker containers for integration testing.
- [**Reactor Test**](https://projectreactor.io/docs/core/release/reference/index.html#testing) - Testing module for the Reactor library, facilitating the testing of reactive code.
- [**Buildpacks**](https://buildpacks.io) - Used to build OCI-compliant images for streamlined deployment and scalability.
### Lessons Learned
Throughout the development, I have gained valuable knowledge and experience in several areas. This includes working with the Discord API and Discord4J library, implementing reactive programming using Project Reactor, managing a PostgreSQL database, setting up a CI/CD pipeline, conducting comprehensive testing with JUnit 5 and Mockito, leveraging Docker containers for integration testing using Testcontainers, and utilizing buildpacks for building OCI-compliant images. This project has provided me with practical hands-on experience in building a robust Discord bot and honing my skills in various Java technologies and frameworks, as well as modern deployment practices.


## Getting Started

Follow these instructions to get EventPilot up and running in your Discord server.

### Prerequisites
If you intend to only run bot on your local machine, you need:
- Docker

Before you begin to code, make sure your development environment have the following:
- Java Development Kit (JDK) 17 or higher
- Maven
- Docker

### Run application locally in docker
One of the approach to run bot locally is that we can use docker container to run both application and database with ease. 
Preferable approach is to run docker-compose.yml file, to make sure all the configuration is set correctly. Example of working [docker-compose.yml](https://github.com/havlli/EventPilot/blob/main/docker-compose.yml) from this repository.
```yaml
name: eventpilot
services:
  spring-boot-app:
    container_name: eventpilot
    image: havlli/eventpilot:latest
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://database:5432/eventpilot
      DISCORD_TOKEN: your-discord-bot-token
    ports:
      - 8080:8080
    depends_on:
      - database
  database:
    container_name: postgres
    image: postgres:15-alpine
    environment:
      POSTGRES_DB: eventpilot
      POSTGRES_USER: havlli
      POSTGRES_PASSWORD: password
    volumes:
      - database:/data/postgres
    ports:
      - 5554:5432
    restart: unless-stopped

volumes:
  database:
```
1. Create or download docker-compose.yml file with same structure presented above.

Make sure you replace `DISCORD_TOKEN` environmental variable with your discord bot token. If you're unsure how to obtain the discord bot token check [FAQ: How to acquire Discord Bot Token](#how-to-acquire-discord-bot-token)

Once you set your `DISCORD_TOKEN` variable simply run in same directory:
```shell
docker compose up -d
```
2. Done! After docker successfully creates and runs both containers your bot should appear online in your server and listening to commands.

_Note: If your bot is not present in the server you have to first invite the bot. [FAQ: How to invite newly created bot to my Discord server](#how-to-invite-newly-created-bot-to-my-discord-server)_

### Setup Development Environment
1. CD to your project folder and clone repository.
```shell
cd projectdirectory
git clone https://github.com/havlli/EventPilot.git
```
2. Rename `.env.example` file to `.env` and set the environmental variables, no need to export environmental variables into your system since `spring-dotenv` manages importing env variables from `.env` file.
```dotenv
DISCORD_BOT_TOKEN=YOUR_DISCORD_BOT_TOKEN
TEST_DISCORD_BOT_TOKEN=ANOTHER_DISCORD_BOT_TOKEN_FOR_TESTING
```
_Note: You can use same token for the test token, you should be running the development environment on testing discord bot instance. In case you're running dev environment on "live" discord server you can use different token for testing Discord4J and Discord API calls._

3. In terminal navigate to `docker-compose-postgres.yml` and compose postgresql container in detach mode.
```shell
cd src/main/resources/db
docker compose -f docker-compose-postgres.yml up -d
```
_Note: If you make any changes to postgres container make sure the changes reflects in application properties `application.yml` as well._

4. Done! You're up and ready to start coding!

## FAQ
### How to acquire Discord Bot Token
1. Navigate to **[Discord Developer Portal](https://discord.com/developers/)** and login.
2. Click on **'New Application'** to create a new application.
3. Give your application a name and click **'Create'**.
4. In the left sidebar, click on **'Bot'**.
5. Under the **Token** section, click on **'Reset Token'** and then **'Copy'** to copy the bot token to your clipboard.

Note: Treat your bot token as a secret and keep it secure. Do not share it publicly or commit it to version control. You can keep your token in .env file. Application loads environmental variables from that file.

### How to invite newly created bot to my Discord server
To invite your created bot to your Discord server, you'll need the **"Manage Server"** permission or have an account with the necessary permissions on the Discord server where you want to add the bot.
1. Navigate to **[Discord Developer Portal](https://discord.com/developers/)** and login.
2. Select your bot application.
2. In the left sidebar, click on **'OAuth2'**, then click on **'URL Generator'**.
3. Under **Scopes** make sure that **'bot'** and **'applications.commands'** values are checked.
4. Under **Bot Permissions** make sure that **'Administrator'** value is checked.
5. Copy the generated URL and open it in your web browser.
6. You will be prompted to authorize the bot. Select the Discord server where you want to add the bot and authorize it.
7. Complete any additional verification steps if prompted.
8. The bot will now be added to your Discord server.

<!-- Repository -->
[repo_url]: https://github.com/havlli/EventPilot
[logo_png_url]: https://raw.githubusercontent.com/havlli/EventPilot/main/public/logo-300px.png
[logo_svg_url]: https://raw.githubusercontent.com/havlli/EventPilot/main/public/logo.svg