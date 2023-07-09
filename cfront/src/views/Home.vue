<template>
    <div class="wrapper">
        <!--        第一步-->
        <v-header/>

        <!--        第二步-->
        <v-sidebar/>

        <!--        第三步-->
        <!--        <div class="content-box">-->
        <!--            <div class="content">-->
        <!--                <transition name="move" mode="out-in">-->
        <!--                    <router-view></router-view>-->
        <!--                </transition>-->
        <!--            </div>-->
        <!--        </div>-->

        <!--        第四步-->
        <div class="content-box" :class="{'content-collapse':collapse}">
            <div class="content">
                <transition name="move" mode="out-in">
                    <router-view></router-view>
                </transition>
            </div>
        </div>
    </div>
</template>

<script>
    //这么命名是为了防止和html本身的标签起冲突
    import vHeader from '../components/Header.vue';
    import vSidebar from '../components/Sidebar.vue';
    import {queryBalance, queryOrder, queryPosi, queryTrade} from "@/api/orderApi";
    import vue from '../main';
    import {codeFormat} from "@/api/formatter";


    export default {
        data() {
            return {
                tagsList: [],
                collapse: false,
            };
        },
        components: {
            vHeader,
            vSidebar
        },
        created() {

            this.$bus.on('collapse-content', msg => {
                this.collapse = msg;
            });
            this.$bus.on("tradechange", res=>{
              let jres = JSON.parse(res);
              let msg = "已成" + (jres.direction == "BUY"?"买入":"卖出")
                  + codeFormat(jres.code) + " " + jres.volume + "股";
              this.$notify({
                title: '新成交',
                message: msg,
                position: 'bottom-right',
                type: 'success'
              })
            })
        },
        beforeDestroy() {
            this.$bus.off("collapse-content", msg=> {
              this.collapse = msg;
            });
        },
        eventbus:{
          handlers:[
            {
              address:"orderchange-"+sessionStorage.getItem("uid"),
              headers:{},
              callback:function (err,msg){
                queryOrder();

                queryBalance();

                queryTrade();

                queryPosi();

              }
            },
            {
              address:"tradechange-" + sessionStorage.getItem("uid"),
              headers: {},
              callback: function (err,msg){
                vue.$bus.emit("tradechange",msg.body);
              }
            }
          ]
        }
    }

</script>
