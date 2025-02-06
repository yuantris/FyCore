## FyCore
> 项目初始化

> `Android.initialize(this)`
>
> `
> // 设置重启对象
> Android.homeActivity = MainActivity::class.java
> `

**模块概览**
- 应用配置
  - 首页toolbar连点3次，第4次长按打开应用配置设置
    - 启动动画设置

**模块功能**
- 常用拓展函数速览
  - 

- 三方库
  - BRV(https://github.com/liangjingkanji/BRV)
  - LiveEventBus(https://github.com/michaellee123/LiveEventBus)
  - ShapeView(https://github.com/getActivity/ShapeView)
  - StateLayout(https://github.com/liangjingkanji/StateLayout)
  - 

- LifecycleHelp
  - Lifecycle管理器,管理项目中Activity、service的状态


**注意事项**

- 事件总线
    - sentEvent与receiveEvent方法绑定使用
    - postEvent与observeEvent方法绑定使用(推荐使用)
  
- 用户首选项
    - Preferences.getValue
    - Preferences.setValue

- Room
  - `BaseDao`
      - 用与封装常用如增删改查的Dao语句
  - `Repository`
      - 数据库数据处理中心的作用
      - 像一些复杂列表数据处理，事务查询等都放在这里面
  - `VMFactory`
      - 这是创建ViewModel的工厂
      - VM里面是处理UI数据的
      - 负责将`Repository`里面的数据发送给页面，这样UI层只通过VM获取数据

- BaseActivity与BaseFragment
    - 接管返回键方法onBackPressedCall的使用
      - 需要在initial方法super之前加入setTakeOverBackPressed来开启/关闭接管
      - 当开启接管时，onBackPressedCall方法生效
      
- BaseBottomSheetDialog
    - initConfig(builder: Builder)
      - 子类重写该方法获取builder可进行进行额外的配置
    - initView()
      - 子类重写该方法进行初始化View
