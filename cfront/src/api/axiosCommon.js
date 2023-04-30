//把一个js变量转化为url参数
import Qs from 'qs';

//包装了ajax，方便http调用
import axios from 'axios';

//通用公共方法（包含回调）
export const reqRealEndAsync = (method,baseUrl,url,params,callback)=>{
    return axios({
        timeout:5000,
        baseUrl:baseUrl,
        method:method,
        url:url,
        headers:{
            'Content-type':'application/x-www-form-urlencoded',
        },
        data:Qs.stringify(params),
        //false --会对data进行深度序列化，后端需要对其进行序列化
        //true --List<String>在框架中可以直接拿到list
        traditional:true,
    }).then(res=>{
        let result = res.data;
        if(result.code == 1){
            router.replace()
        }else if(result.code ==0){

        }else{

        }
    });
}
//通用公共方法（不包含回调）
export const reqRealEndAsync = (method,baseUrl,url,params)=>{

}