<template>
    <!--  持仓列表  -->
    <div>
        <el-row>
            <el-col :span="5">
                可用资金:{{balance}}
            </el-col>
        </el-row>

        <el-table
                :data="
                    tableData.slice
                    (
                        (query.currentPage - 1) * query.pageSize,
                        query.currentPage * query.pageSize
                    )
                "
                border
                :cell-style="cellStyle"
                @sort-change="changeTableSort"
        >
            <el-table-column prop="code" label="代码" align="center"
                             sortable :sort-orders="['ascending', 'descending']"
                             :formatter="codeFormatter"
            />
            <el-table-column prop="name" label="名称" align="center"/>
            <el-table-column prop="count" label="股票数量" align="center"/>
            <el-table-column prop="cost" label="总投入" align="center" :formatter="moneyFormatter"/>
            <el-table-column label="成本" align="center" :formatter="costFormatter"/>
        </el-table>

        <div class="pagination">
            <el-button round
                       type="primary" size="mini"
                       style="margin-top:2px;float: right"
                       icon="el-icon-refresh"
                       @click="queryRefresh">
                刷新
            </el-button>
            <el-pagination
                    background
                    layout="total, prev, pager, next"
                    :current-page="query.currentPage"
                    :page-size="query.pageSize"
                    :total="dataTotalCount"
                    @current-change="handlePageChange"/>
        </div>

    </div>
</template>

<script>
    import {constants} from "@/api/constants";
    import {codeFormat,moneyFormat} from "@/api/formatter";
    import {queryBalance, queryPosi} from "@/api/orderApi";

    export default {
        name: "PosiList",
        data() {
            return {
                tableData: [],
                dataTotalCount: 0,

                balance: 0,

                query: {
                    currentPage: 1, // 当前页码
                    pageSize: 2 // 每页的数据条数
                }
            };
        },
        methods: {
            queryRefresh(){
              queryPosi();
              queryBalance();
              console.log("3")
            },
            //成本转换器
            costFormatter(row,column){
              return(row.cost/constants.MULTI_FACTOR/row.count).toFixed(2);
            },
            //资金格式化
            moneyFormatter(row,column){
              return moneyFormat(row.cost);
            },
            codeFormatter(row,column){
              return codeFormat(row.code);
            },
            // 分页导航
            handlePageChange(val) {
                this.$set(this.query, 'currentPage', val);
            },

            //处理排序
            changeTableSort(column) {
                console.log('600886' - '000001');
                let fieldName = column.prop;
                if (column.order == "descending") {
                    this.tableData = this.tableData.sort((a, b) => b[fieldName] - a[fieldName]);
                } else {
                    this.tableData = this.tableData.sort((a, b) => a[fieldName] - b[fieldName]);
                }
            },

            cellStyle({row, column, rowIndex, columnIndex}) {
                    return "padding:2px";
            },
        },
        computed: {
          posiData(){
            return this.$store.state.posiData;
          },
          balanceData(){
            return moneyFormat(this.$store.state.balanceData);
          }
        },
        //vuex中变量发生变化时computed中计算属性也会发生变化，这时watch就会自动执行
        watch: {
          posiData:function (val){
            this.tableData=val;
            this.dataTotalCount = val.length;
          },
          balanceData:function (val){
            this.balance = val;
          }
        },
        created() {
          queryPosi();
          queryBalance();
          this.tableData = this.posiData;
          this.balance = this.balanceData;
        }
    }
</script>

<style scoped>

</style>