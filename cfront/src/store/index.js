import Vue from 'vue'
import Vuex from 'vuex'

Vue.use(Vuex)

export default new Vuex.Store({
  state: {
    posiData:[],
    orderData:[],
    tradeData:[],
    balanceData: '',
  },
  mutations: {

    updatePosi(state,posiInfo){
      state.posiData=posiInfo;
      sessionStorage.setItem("posidata",posiInfo)
    },
    updateOrder(state,orderInfo){
      state.orderData=orderInfo;
      sessionStorage.setItem("orderdata",orderInfo)
    },
    updateTrade(state,tradeInfo){
      state.tradeData=tradeInfo;
      sessionStorage.setItem("tradedata",tradeInfo)
    },
    updateBalance(state,balance){
      state.balanceData=balance;
      sessionStorage.setItem("balancedata",balance)
    },
  },
  //异步操作
  // actions: {
  // },
  //类似state
  // modules: {
  // }
})
