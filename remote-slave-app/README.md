
## Installation Guide

### Installing Brew

#### Automatic Installation

```
curl https://http-static-server.pharmeasy.in/device-lab-files/install-slave.bash -o install-slave.bash && bash install-slave.bash
```
OpenSTF logs can be found at **`/usr/local/share/device-lab/stf.log`**

Slave Logs can be found at **`/usr/local/share/device-lab/slave.log`**

If any error occurred during installation, follow the below steps to insttall manually.

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

### Installing Appium

```
npm install -g appium
```

#### Android SDK Configuration

* Install Android Studio from [https://developer.android.com/studio](https://developer.android.com/studio)
* From SDK Manager → SDK Tools, install ***Android Emulator, Android SDK Platform-Tools, Android SDK Tools*** 
* From AVD Manager → Create ***New Virtual Device***, if required

Once this installation is done, Android Studio is not required anymore. It can be deleted.

Also, make sure **`ANDROID_HOME`** environment variable is set. To know the path, open ***Android Studio*** → ***Configure*** (appear on lower bottom corner) → ***SDK Manager*** → ***Copy Android SDK Location***

```
sudo ln -s $ANDROID_HOME /opt/android-sdk
```

#### On Macintosh Machines Only

```
brew install libimobiledevice --HEAD
brew install ideviceinstaller
brew install ios-deploy
brew install carthage
brew tap facebook/fb
brew install fbsimctl --HEAD
```

For real ios devices configuration, refer this [link](http://appium.io/docs/en/about-appium/getting-started/)


### Installing OpenSTF

[OpenSTF](https://github.com/openstf/stf) is a web application for debugging smartphones, smartwatches and other gadgets remotely, from the comfort of your browser.

#### Installing Node Version Manager (NVM)

[NVM](https://github.com/nvm-sh/nvm) is necessary for OpenSTF installation because it works with **`node version <= 8`** as one of OpenSTF dependencies won't work with higher version of node.

```
brew update
brew install nvm
mkdir ~/.nvm
```

Add the following 2 lines in `~/.bash_profile` and run the next set to commands.

```
export NVM_DIR=~/.nvm
source $(brew --prefix nvm)/nvm.sh
```

```
source ~/.bash_profile
nvm install 8.16.1
```

#### Installing STF

```
brew install rethinkdb graphicsmagick zeromq protobuf yasm pkg-config
brew services rethinkdb start
npm install -g stf
```

#### Starting OpenSTF Locally

```
stf local
```

If you want to access STF from other machines, you can add the **`--public-ip`**

```
stf local --public-ip <your_internal_network_ip_here>
```

To know the IP, run the below command and look for **`172.`** or **`10.`**

```
ifconfig
```

### Building the Remote Slave Application

```
./gradlew clean build -x test
```

After building, we should be able to see the following file **`$PROJECT_DIR/build/libs/remote-slave-app.war`**


### Running the Application as Background Process

```
java -jar $PROJECT_DIR/build/libs/remote-slave-app.war --master.host=0.0.0.0 > /tmp/remote-slave-app.log 2>&1 < /dev/null &
```

**--master.host=0.0.0.0** arguments is a mandatory argument that denotes the master to which slave should connect to. 
