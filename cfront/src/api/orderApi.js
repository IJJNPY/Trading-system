import {reqRealEnd, reqRealEndAsync} from "@/api/axiosCommon";

import {config} from "@/api/frontConfig";

import store from "@/store";

//查看资金
export const queryBalance = () =>{
    reqRealEndAsync("post",config.real_domain,'api/balance',
        {uid:sessionStorage.getItem('uid')},
        (code,msg,data)=>{
            store.commit("updateBalance",data)
        })
}

//查看持仓
export const queryPosi = () =>{
    reqRealEndAsync("post",config.real_domain,
        'api/posiinfo',
        {uid:sessionStorage.getItem('uid')},
        (code,msg,data)=>{
            store.commit("updatePosi",data)
        })
}

//查看委托
export const queryOrder = () =>{
    reqRealEndAsync("post",config.real_domain,
        'api/orderinfo',
        {uid:sessionStorage.getItem('uid')},
        (code,msg,data)=>{
            store.commit("updateOrder",data)
        })
}

//查看交易
export const queryTrade = () =>{
    reqRealEndAsync("post",config.real_domain,
        'api/tradeinfo',
        {uid:sessionStorage.getItem('uid')},
        (code,msg,data)=>{
            store.commit("updateTrade",data)
        })
}

//查股票
export const queryCodeName = (params) =>{
    return reqRealEnd("post",config.real_domain,'/api/code',params)
}

//发送委托
export const sendOrder = (params,callback) =>{
    return reqRealEndAsync("post", config.real_domain, 'api/sendorder',params,callback);
}