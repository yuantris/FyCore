## FyCore
> 项目初始化:
> `Android.initialize(this)`
> `*// 设置重启对象*`
> `*Activity*Android.homeActivity = MainActivity::class.*java*`

**注意事项**

- 事件总线
    - sentEvent与receiveEvent方法绑定使用
    - postEvent与observeEvent方法绑定使用(推荐使用)