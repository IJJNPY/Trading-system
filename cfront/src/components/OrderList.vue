<template>
    <!--  委托列表  -->
    <div>
        <el-table
                :data="tableData.slice( (query.currentPage - 1) * query.pageSize, query.currentPage * query.pageSize)"
                border
                :cell-style="cellStyle"
                style=" width: 100%;font-size: 14px;"
                :default-sort="{prop:'time',order:'descending'}"
                @sort-change="changeTableSort"
        >
            <el-table-column prop="time" label="委托时间" align="center"
                             sortable :sort-orders="['ascending', 'descending']"/>
            <el-table-column prop="code" label="股票代码" align="center"/>
            <el-table-column prop="name" label="名称" align="center"/>
            <el-table-column prop="price" label="委托价格" align="center"/>
            <el-table-column prop="ocount" label="委托数量" align="center"/>
            <el-table-column prop="direction" label="方向" align="center"/>
            <el-table-column prop="status" label="状态" align="center"/>
            <el-table-column width="85">
                <template slot-scope="scope">
                    <el-button
                            v-show="isCancelBtnShow(scope.row.status)"
                            type="primary"
                            size="mini"
                            @click="handleCancel(scope.$index, scope.row)"
                    >撤单
                    </el-button>
                </template>
            </el-table-column>
        </el-table>
        <div class="pagination">
            <el-button round
                       type="primary" size="mini"
                       style="margin-top:2px;float: right"
                       icon="el-icon-refresh"
                       @click="">
                刷新
            </el-button>
            <el-pagination
                    background
                    layout="total, prev, pager, next"
                    :current-page="query.currentPage"
                    :page-size="query.pageSize"
                    :total="3"
                    @current-change="handlePageChange"/>
        </div>
    </div>
</template>

<script>

    import {queryOrder, queryBalance, cancelOrder} from "../api/orderApi";
    import {codeFormat, moneyFormat, directionFormat, statusFormat} from "../api/formatter";
    import {constants} from "../api/constants";

    export default {
        name: "OrderList",
        data() {
            return {
                tableData: [],
                query: {
                    currentPage: 1, // 当前页码
                    pageSize: 4 // 每页的数据条数
                }
            };
        },
        computed: {
          orderData() {
            return this.$store.state.orderData;
          },
          dataTotalCount() {
            return this.$store.state.orderData.length;
          }
        },
        watch: {
          orderData: function (val) {
            this.tableData = val;
          }
        },
        created() {
          this.tableData = this.orderData;
        },
        methods: {
            isCancelBtnShow(status) {
                if (status == 3 || status == 5) {
                    return true;
                } else {
                    return false;
                }
            },
            handleCancel(index, row) {
              let message = (row.direction === constants.BUY ? "买入" : "卖出")
                  + "     " + row.name + "(" + codeFormat(row.code) + ")    "
                  + row.ocount + "股";
              this.$confirm(message, '撤单', {
                confirmButtonText: '确定',
                cancelButtonText: '取消',
                type: 'warning'
              }).then(() => {
                cancelOrder(
                    {
                      uid: sessionStorage.getItem("uid"),
                      counteroid: row.id,
                      code: row.code
                    },
                    undefined);
              });

            },
            queryOrder() {
              queryOrder();
              queryBalance();
            },
            codeFormatter(row, column) {
              return codeFormat(row.code);
            },
            priceFormatter(row, column) {
              return moneyFormat(row.price);
            },
            directionFormatter(row, column) {
              console.log(row);
              return directionFormat(row.direction);
            },
            // 禁用状态格式化
            statusFormatter(row, column) {
              // 委托状态：// 0.已报  1.已成 2.部成 3.废单 4.已撤
              return statusFormat(row.status);
            },

            cellStyle({row, column, rowIndex, columnIndex}) {
                return "padding:2px;";
            },
            // 分页导航
            handlePageChange(val) {
                this.$set(this.query, 'currentPage', val);
            },
            //处理排序
            changeTableSort(column) {
                let sortingType = column.order;
                let fieldName = column.prop;
                if (fieldName === "time") {
                    if (sortingType == "descending") {
                        this.tableData = this.tableData.sort((a, b) => {
                                if (b[fieldName] > a[fieldName]) {
                                    return 1;
                                } else if (b[fieldName] === a[fieldName]) {
                                    return 0;
                                } else {
                                    return -1;
                                }
                            }
                        );
                    } else {
                        this.tableData = this.tableData.sort((a, b) => {
                            if (b[fieldName] > a[fieldName]) {
                                return -1;
                            } else if (b[fieldName] === a[fieldName]) {
                                return 0;
                            } else {
                                return 1;
                            }
                        });
                    }
                }
            }
        },
    }
</script>

<style scoped>

</style>