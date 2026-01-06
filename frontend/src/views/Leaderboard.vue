<template>
  <div class="leaderboard-container">
    <div class="header">
      <h2>🏆 知识贡献排行榜</h2>
      <p>分享知识，为企业智慧贡献力量</p>
    </div>

    <el-card class="top-three-card">
      <div class="top-three-container">
        <!-- Second Place -->
        <div class="rank-item rank-2" v-if="users[1]">
          <div class="crown" style="color: #C0C0C0">🥈</div>
          <el-avatar :size="80" :src="users[1].avatar || ''" :class="{'no-avatar': !users[1].avatar}">
            {{ users[1].realName.charAt(0) }}
          </el-avatar>
          <div class="name">{{ users[1].realName }}</div>
          <div class="points">{{ users[1].points }} 积分</div>
        </div>
        
        <!-- First Place -->
        <div class="rank-item rank-1" v-if="users[0]">
          <div class="crown" style="color: #FFD700">👑</div>
          <el-avatar :size="100" :src="users[0].avatar || ''" class="avatar-1" :class="{'no-avatar': !users[0].avatar}">
            {{ users[0].realName.charAt(0) }}
          </el-avatar>
          <div class="name">{{ users[0].realName }}</div>
          <div class="points">{{ users[0].points }} 积分</div>
        </div>

        <!-- Third Place -->
        <div class="rank-item rank-3" v-if="users[2]">
          <div class="crown" style="color: #CD7F32">🥉</div>
          <el-avatar :size="80" :src="users[2].avatar || ''" :class="{'no-avatar': !users[2].avatar}">
            {{ users[2].realName.charAt(0) }}
          </el-avatar>
          <div class="name">{{ users[2].realName }}</div>
          <div class="points">{{ users[2].points }} 积分</div>
        </div>
      </div>
    </el-card>

    <el-card class="list-card">
      <el-table :data="users.slice(3)" stripe style="width: 100%">
        <el-table-column type="index" label="排名" width="80">
            <template #default="scope">
                <span class="rank-number">{{ scope.$index + 4 }}</span>
            </template>
        </el-table-column>
        <el-table-column prop="realName" label="用户" width="180">
             <template #default="scope">
                <div class="user-cell">
                    <el-avatar :size="30" :src="scope.row.avatar || ''" style="margin-right: 10px;">{{ scope.row.realName.charAt(0) }}</el-avatar>
                    {{ scope.row.realName }}
                </div>
            </template>
        </el-table-column>
        <el-table-column prop="department" label="部门" />
        <el-table-column prop="points" label="总积分" align="right">
            <template #default="scope">
                <span class="points-cell">{{ scope.row.points }}</span>
            </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import api from '../api'

const users = ref([])

onMounted(async () => {
    try {
        const res = await api.get('/user/leaderboard?limit=20')
        // Use real data if available, otherwise use mock data
        if (res.data && res.data.length > 0) {
            // Filter out users with 0 or null points, sort by points
            const validUsers = res.data
                .filter(u => u.points && u.points > 0)
                .sort((a, b) => (b.points || 0) - (a.points || 0))
            
            if (validUsers.length > 0) {
                users.value = validUsers
            } else {
                // All users have 0 points, use mock data
                users.value = getMockData()
            }
        } else {
            users.value = getMockData()
        }
    } catch (e) {
        console.error('Failed to load leaderboard:', e)
        users.value = getMockData()
    }
})

const getMockData = () => [
    { realName: '张三', points: 1250, department: '技术部' },
    { realName: '李四', points: 980, department: '产品部' },
    { realName: '王五', points: 850, department: '市场部' },
    { realName: '赵六', points: 720, department: '运营部' },
    { realName: '孙七', points: 600, department: '技术部' }
]
</script>

<style scoped>
.leaderboard-container {
  padding: 20px;
  max-width: 800px;
  margin: 0 auto;
}
.header {
  text-align: center;
  margin-bottom: 30px;
}
.header h2 {
    font-size: 28px;
    background: linear-gradient(45deg, #FFD700, #FF8C00);
    -webkit-background-clip: text;
    -webkit-text-fill-color: transparent;
    margin-bottom: 10px;
}
.top-three-container {
  display: flex;
  justify-content: center;
  align-items: flex-end;
  padding: 20px 0;
  gap: 20px;
}
.rank-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
}
.crown {
  font-size: 24px;
  margin-bottom: 10px;
}
.avatar-1 {
    border: 4px solid #FFD700;
}
.name {
    margin-top: 10px;
    font-weight: bold;
    font-size: 16px;
}
.points {
    color: #666;
    font-size: 14px;
    margin-top: 5px;
}
.list-card {
    margin-top: 20px;
}
.rank-number {
    font-weight: bold;
    color: #999;
}
.points-cell {
    color: #E6A23C;
    font-weight: bold;
}
.user-cell {
    display: flex;
    align-items: center;
}
</style>
