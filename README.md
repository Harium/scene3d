# scene3d
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.harium.gdx/scene3d/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.harium.gdx/scene3d/)

A libGDX library to display scene2d in a 3d environment.

Current libGDX version: **1.10.0**

You can check how it looks, in the [DesktopExample](https://github.com/Harium/scene3d/blob/main/src/test/java/DesktopExample.java).

## Install

```
allprojects {
	ext {
        ...
        scene3dVersion = '0.0.2'
    }
}
```

Add dependency in your core project (replace -SNAPSHOT by latest release to use a stable version) :

```
project(":core") {
    dependencies {
    	...
        api "com.harium.gdx:scene3d:$scene3dVersion"
    }
}
```

For GWT (html) projects you need to add source dependency and inherit GWT module in your core .gwt.xml file.

```
project(":html") {
    dependencies {
    	...
        api "com.harium.gdx:scene3d:$scene3dVersion:sources"
    }
}
```

```
<module>
	<inherits name='Scene3d' />
	...
</module>
```



