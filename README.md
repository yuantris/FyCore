## FyCore
> 项目初始化

> `Android.initialize(this)`
>
> `
> // 设置重启对象
> Android.homeActivity = MainActivity::class.java
> `

**模块介绍**
- 应用配置
  - 首页toolbar连点3次，第4次长按打开应用配置设置
    - 启动动画设置


**注意事项**

- 事件总线
    - sentEvent与receiveEvent方法绑定使用
    - postEvent与observeEvent方法绑定使用(推荐使用)
  
- 用户首选项
    - Preferences.getValue
    - Preferences.setValue

- BaseActivity与BaseFragment
    - 接管返回键方法onBackPressedCall的使用
      - 需要在initial方法super之前加入setTakeOverBackPressed来开启/关闭接管
      - 当开启接管时，onBackPressedCall方法生效
