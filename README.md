# specialagent-spring-boot

It looks like if your application loads OpenTracing classes before the agent is attached you app will break. Run it with

```
mvn spring-boot:run
```

Uncomment https://github.com/william-tran/specialagent-spring-boot/blob/master/src/main/java/com/example/specialagentdemo/SpecialagentDemoApplication.java#L89-L94 and the requests will fail.
