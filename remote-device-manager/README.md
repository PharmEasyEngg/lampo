## Installation Guide

### Installating Brew

#### Macintosh

```
/bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/master/install.sh)"

```

#### Linux

```
/bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/master/install.sh)"

test -d ~/.linuxbrew && eval $(~/.linuxbrew/bin/brew shellenv)
test -d /home/linuxbrew/.linuxbrew && eval $(/home/linuxbrew/.linuxbrew/bin/brew shellenv)
test -r ~/.bash_profile && echo "eval \$($(brew --prefix)/bin/brew shellenv)" >>~/.bash_profile
echo "eval \$($(brew --prefix)/bin/brew shellenv)" >>~/.profile

```

### Installing MongoDB

```
brew tap mongodb/brew
brew install mongodb-community
brew services start mongodb-community
```

### Installing RabbitMQ

```
brew install rabbitmq
brew services start rabbitmq
```

### Building the Remote Device Manager Application

```
./gradlew clean build -x test
```

After building, we should be able to see the following file **`$PROJECT_DIR/build/libs/remote-device-manager.jar`**

### Running the Application as Background Process

```
java -jar $PROJECT_DIR/build/libs/remote-device-manager.jar > /tmp/remote-device-manager.log 2>&1 < /dev/null &
```