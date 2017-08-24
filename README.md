# MethodTimeCost
A gradle plugin to show time costs of methods on `main-thread`.

## How to use
Just two lines.

One is add dependency in the build.gradle of the `root project`.

    dependencies {
        classpath 'com.xiaobo.plugin:method-time-cost:1.0.2'

        ...
    }

The other one is apply the plugin in the build.gradle of your `application module`. The extension `method_time_cost` is optional.

    apply plugin: 'com.xiaobo.method_time_cost'
    method_time_cost {
        enable = true           // optional, default is true.
        costBiggerThan = 20     // optional, default is 0. ==> show log of methods cost time >= 0
        tag = "xiaobo"          // optional, default is MethodTimeCost
    }

## How it works

    public int add (int a, int b) {
        int result = a + b;         // imagine this is line 3
        return result;
    }

will change into

    public int add (int a, int b) {
        TimeUtil.push();            // now this is line 3
        int result = a + b;         // changed to line 4
        TimeUtil.pop();
        return result;
    }

where `pop` time minus `push` time is the time 'add' costs. The code is inserted by Java Assist.

## Suggestion
Just use the plugin under debug mode. For it changed the byte code of your classes, which results in a wrong `line number` if you want to locate a right line number from stacktrace when you get an exception.

## Others
1. A few methods can not be monitored since the exception from Java Assist.
2. You can use `AspectJ` instead. But you will write more code and it will generate `too many` classes.