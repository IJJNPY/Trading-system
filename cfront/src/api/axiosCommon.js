import router from '../router'
//把一个js变量转化为url参数
import Qs from 'qs';

//包装了ajax，方便http调用，通过http的方式访问后台
import axios from 'axios';

//通用公共方法（包含回调）
//method 使用post方式去调用还是用get的方式
//baseurl 当前柜台服务存放的地方
//url 当前请求的服务
//params 请求这个服务需要的参数
//callback 请求返回之后需要什么样的处理，回调函数
export const reqRealEndAsync = (method,baseUrl,url,params,callback)=>{

    // console.log(Qs.stringify(params));

    //使用axios调用后台接口
    params.token = sessionStorage.getItem('token');

    return axios({
        timeout:5000,
        baseURL:baseUrl,
        method:method,
        url:url,
        // http的消息头
        headers:{
            'Content-type':'application/x-www-form-urlencoded',
        },
        data:Qs.stringify(params),

        //false --会对data进行深度序列化，后端需要对其进行序列化
        //true --List<String>在框架中可以直接拿到list
        traditional:true,
        // transformRequest: [function (params) {
        //     return Qs.stringify(params);
        // }]
    }).then(res=>{
        // console.log(res)
        // console.log("out")
        let result = res.data;
        //code:0 -- success
        if(result.code == 1){
            //验证失败，页面跳转
            router.replace({
                path:"login",
                query:{
                    msg: result.message
                }
            })
        }else if(result.code ==0){
            if(callback!=undefined){
                callback(result.code,result.message,result.data);
            }
        }else if(result.code == 2){
            if(callback!=undefined){
                callback(result.code,result.message,result.data)
            }
        }else{
            console.error(result);
        }
    });
}

//通用公共方法（不包含回调）
export const reqRealEnd = (method,baseUrl,url,params)=>{
    params.token = sessionStorage.getItem('token');
    return axios({
        timeout:5000,
        baseURL:baseUrl,
        method:method,
        url:url,
        headers:{
            'Content-type':'application/x-www-form-urlencoded',
        },
        data:Qs.stringify(params),
        //false --会对data进行深度序列化，后端需要对其进行序列化
        //true --List<String>在框架中可以直接拿到list
        traditional:true,
    })
}