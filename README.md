# java-dependency-manager
Java 版本升级简易工具，旨在根据预设特定依赖的版本和操作建议快速输出升级建议。

本工具基于SpringBoot+Bootstrap+JQuery+Shell脚本实现，目前仅支持Gradle工程，择期支持Maven工程。

**注意事项：**<br/>
1）dependency-update.json 依赖升级配置元数据，标明特定依赖升级的操作及建议，支持：新增/更新/排除操作<br/>
2）dependency.sh Shell脚本，核心功能是git clone Java Gradle工程并执行 gradle dependencies 命令，在正式使用时请确保此shell脚本具有可执行权限（**chmod +x dependency.sh**）

**其他说明：**<br/>
1）本工程仅仅是为了方便日常工作中，大量项目需要升级基础组件的情况，设计时可能未考虑过多场景，使用时请根据具体场景改造后使用<br/>
2）本工程为个人业余时间所做，不保证执行结果的绝对准确，不承担任何可能基于此产生的后果<br/>
3）感兴趣的同学可以加入websocket+scp特性等，实现一个简易工程发布系统



