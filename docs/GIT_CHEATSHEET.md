# 🧾 Git & GitHub Cheatsheet for OptiGro Team

> **For beginners who have never used Git before.**  
> **Repository:** [https://github.com/Vishal-Jadoun/OptiGro.git](https://github.com/Vishal-Jadoun/OptiGro.git)

---

## 📥 Step 0: One-Time Setup

### Install Git
1. Download from [https://git-scm.com/downloads](https://git-scm.com/downloads)
2. Install with default settings (keep clicking Next)
3. Open **Git Bash** (or terminal/command prompt)

### Configure your identity (do this ONCE):
```bash
git config --global user.name "Your Full Name"
git config --global user.email "your.email@gmail.com"
```

### Clone the repository (do this ONCE):
```bash
# Navigate to where you want the project folder
cd C:/Users/YourName/Desktop

# Clone the repo
git clone https://github.com/Vishal-Jadoun/OptiGro.git

# Enter the project folder
cd OptiGro
```

Now you have a local copy of the project! 🎉

---

## 🌿 Step 1: Create Your Branch

> **NEVER work directly on `main`.** Always create and work on your own branch.

### What is a branch?
Think of `main` as the "clean copy" of the project. Your branch is your personal workspace where you can make changes without affecting others.

```
main ──────────────────────────── (always working, clean)
  │
  ├── m1/inventory ────────────── (Vishal works here)
  ├── m2/customer-orders ──────── (Member 2 works here)
  ├── m3/picking-engine ───────── (Member 3 works here)
  └── m4/delivery-platform ────── (Member 4 works here)
```

### Create your branch:
```bash
# Make sure you're on main first
git checkout main

# Pull latest changes
git pull origin main

# Create and switch to your branch
git checkout -b m2/customer-orders    # ← Replace with YOUR branch name
```

**Branch names for each member:**
| Member | Branch Name |
|--------|-------------|
| M1 (Vishal) | `m1/inventory` |
| M2 (Harnoor) | `m2/customer-orders` |
| M3 (Kritika) | `m3/picking-engine` |
| M4 (Aditya) | `m4/delivery-platform` |

You can also create sub-branches for specific tasks:
```bash
git checkout -b m2/pricing          # For working on PricingService
git checkout -b m2/cart-service     # For working on CartService
```

---

## 💾 Step 2: Daily Workflow (Save & Push)

### The 4 commands you'll use every day:

```bash
# 1. Check what you changed
git status

# 2. Stage your changes (add them to the "ready to save" area)
git add .                    # Adds ALL changed files
# OR
git add src/com/optigro/models/Customer.java   # Add specific file

# 3. Commit (save a snapshot with a message)
git commit -m "[M2] add Customer model with wallet logic"

# 4. Push to GitHub (upload your changes)
git push origin m2/customer-orders    # ← YOUR branch name
```

### Good commit message format:
```
[M1] add product hierarchy with abstract base class
[M2] fix GST calculation for SNACKS category
[M3] implement PriorityBlockingQueue for pick tasks
[M4] wire all services in App.java
```

> 💡 **Commit often!** Small, frequent commits are better than one giant commit at the end.

---

## 🔄 Step 3: Get Latest Changes (Pull)

### Before you start working each day:
```bash
# Switch to main
git checkout main

# Get latest from GitHub
git pull origin main

# Switch back to your branch
git checkout m2/customer-orders

# Merge main into your branch (get everyone's updates)
git merge main
```

### If someone else pushed changes to their branch and you need them:
```bash
# Example: M1 pushed utility code you need
git pull origin main    # Only if it's been merged to main already
```

---

## 🔀 Step 4: Merge Your Work to Main (via Pull Request)

### When your sprint is done and code compiles:

#### Option A: Pull Request on GitHub (RECOMMENDED for beginners) ✅

1. Push your branch to GitHub:
   ```bash
   git push origin m2/customer-orders
   ```

2. Go to GitHub → Repository → You'll see a banner:
   ```
   "m2/customer-orders had recent pushes — Compare & pull request"
   ```

3. Click **"Compare & pull request"**

4. Fill in:
   - **Title:** `[M2] Sprint 1 - Customer & Order models`
   - **Description:** What you did, what it adds
   - **Reviewers:** Add Vishal (team lead)

5. Click **"Create pull request"**

6. Vishal (team lead) reviews and clicks **"Merge pull request"**

#### Option B: Direct merge (if you're comfortable)
```bash
# Switch to main
git checkout main

# Pull latest
git pull origin main

# Merge your branch
git merge m2/customer-orders

# Push to GitHub
git push origin main
```

---

## ⚠️ Step 5: Handling Merge Conflicts

### What is a conflict?
A conflict happens when **two people edit the same lines** in the same file. Git doesn't know which version to keep, so it asks you to decide.

### When do conflicts happen?
```
You edited line 10 of DataInitializer.java
Someone else also edited line 10 of DataInitializer.java
→ CONFLICT when you try to merge
```

### How to know you have a conflict:
When you run `git merge main` or `git pull origin main`, you'll see:
```
Auto-merging src/com/optigro/config/DataInitializer.java
CONFLICT (content): Merge conflict in src/com/optigro/config/DataInitializer.java
Automatic merge failed; fix conflicts and then commit the result.
```

### How to fix conflicts:

#### Step 1: Open the conflicted file
The file will have markers like this:

```java
<<<<<<< HEAD
    // YOUR version (what you wrote)
    public static void seedCustomers() {
        customerRepo.save(new Customer(2001, 101, "Rahul", "9876543210", "Delhi", 500));
    }
=======
    // THEIR version (what someone else wrote)
    public static void seedCustomers() {
        customerRepo.save(new Customer(2001, 101, "Rahul Sharma", "9876543210", "New Delhi", 1000));
    }
>>>>>>> main
```

#### Step 2: Decide what to keep
Edit the file to keep the correct version. Remove ALL conflict markers (`<<<<<<<`, `=======`, `>>>>>>>`):

```java
    // RESOLVED: keep the combined version
    public static void seedCustomers() {
        customerRepo.save(new Customer(2001, 101, "Rahul Sharma", "9876543210", "New Delhi", 500));
    }
```

#### Step 3: Mark as resolved and commit
```bash
# Stage the fixed file
git add src/com/optigro/config/DataInitializer.java

# Commit the resolution
git commit -m "[M2] resolve merge conflict in DataInitializer"

# Push
git push origin m2/customer-orders
```

### How to PREVENT conflicts:
1. **Only edit files you own** — This is the #1 rule
2. **Pull `main` before starting work each day**
3. **DataInitializer.java** — Each member only edits their own seed method
4. **Communicate** — If you must edit someone else's file, tell them first

---

## 🛠️ Step 6: Common Situations & Fixes

### "I made changes on the wrong branch!"
```bash
# Save your changes temporarily
git stash

# Switch to the correct branch
git checkout m2/customer-orders

# Apply your saved changes
git stash pop
```

### "I want to undo my last commit (haven't pushed yet)"
```bash
# Undo commit but keep the changes
git reset --soft HEAD~1

# Undo commit and discard changes (CAREFUL!)
git reset --hard HEAD~1
```

### "I want to see what changed"
```bash
# See what files changed
git status

# See the actual line-by-line changes
git diff

# See commit history
git log --oneline

# See commit history with a nice graph
git log --oneline --graph --all
```

### "I want to discard all my local changes and start fresh"
```bash
# Discard all uncommitted changes (CAREFUL - this deletes your work!)
git checkout -- .

# Or reset to match remote
git fetch origin
git reset --hard origin/main
```

### "Git says 'Your branch is behind' when I try to push"
```bash
# This means someone pushed changes you don't have yet
# Pull first, then push
git pull origin m2/customer-orders
# Fix any conflicts if needed (see Step 5)
git push origin m2/customer-orders
```

### "Git says 'Permission denied'"
- Make sure Vishal added you as a **collaborator** on the GitHub repo
- Go to GitHub → Repo → Settings → Collaborators → Add your GitHub username
- Accept the invitation sent to your email

### "I want to see all branches"
```bash
# Local branches
git branch

# All branches (including remote)
git branch -a

# Switch to another branch
git checkout m3/picking-engine     # switches to M3's branch (READ ONLY - don't edit their files)
git checkout m2/customer-orders    # switch back to yours
```

---

## 📋 Quick Reference Card

### Commands You'll Use 90% of the Time

| What You Want to Do | Command |
|---------------------|---------|
| Check status | `git status` |
| Add all changes | `git add .` |
| Commit with message | `git commit -m "[M2] your message"` |
| Push to GitHub | `git push origin YOUR-BRANCH` |
| Get latest from main | `git pull origin main` |
| Switch branch | `git checkout BRANCH-NAME` |
| Create new branch | `git checkout -b NEW-BRANCH-NAME` |
| See history | `git log --oneline` |
| See changes | `git diff` |

### Your Daily Routine

```
Morning:
  1. git checkout main
  2. git pull origin main
  3. git checkout m2/customer-orders
  4. git merge main
  5. (fix conflicts if any)
  6. Start coding!

Before leaving:
  1. git add .
  2. git commit -m "[M2] what you did today"
  3. git push origin m2/customer-orders
```

---

## 🌿 Branch Strategy Diagram

```
main ─────●────────●────────────●────────────●────────────● (always compiles)
          │        ↑            ↑            ↑            ↑
          │    Sprint 1     Sprint 2     Sprint 3     Sprint 4
          │     merge        merge        merge        merge
          │        │            │            │            │
m1/inv ───┼────────●────────────●────────────●────────────● (Vishal)
          │        │            │            │            │
m2/cust ──┼────────●────────────●────────────●────────────● (Member 2)
          │        │            │            │            │
m3/pick ──┼────────●────────────●────────────●────────────● (Member 3)
          │        │            │            │            │
m4/del ───┼────────●────────────●────────────●────────────● (Member 4)
          │
      Day 1: branches created
```

**Rules:**
- `main` is always compilable — never push broken code to it
- Each member works on their own branch
- Merge to `main` at the end of each sprint (after verification)
- Vishal (team lead) manages merges and resolves cross-branch conflicts

---

## 🔐 GitHub Setup (For Vishal — Team Lead)

### 1. Add collaborators
```
GitHub → Repository → Settings → Collaborators → Add people
→ Add all 3 team members' GitHub usernames
```

### 2. Create initial branches
```bash
# After initial commit on main
git checkout -b m1/inventory
git push origin m1/inventory

git checkout main
git checkout -b m2/customer-orders
git push origin m2/customer-orders

git checkout main
git checkout -b m3/picking-engine
git push origin m3/picking-engine

git checkout main
git checkout -b m4/delivery-platform
git push origin m4/delivery-platform

git checkout main
```

### 3. Merge process (Vishal does this at sprint end)
```bash
# Step 1: Get everything latest
git checkout main
git pull origin main

# Step 2: Merge M1 first (foundation layer)
git merge origin/m1/inventory
# Fix conflicts if any
git push origin main

# Step 3: Merge M2
git merge origin/m2/customer-orders
# Fix conflicts if any
git push origin main

# Step 4: Merge M3
git merge origin/m3/picking-engine
# Fix conflicts if any
git push origin main

# Step 5: Merge M4
git merge origin/m4/delivery-platform
# Fix conflicts if any
git push origin main

# Step 6: Verify compilation
javac -d out @sources.txt
# If it fails → fix and commit to main
```

### 4. After merging, tell everyone to update:
```
Team message: "✅ Sprint 1 merged to main. Everyone pull before continuing:
  git checkout main
  git pull origin main
  git checkout YOUR-BRANCH
  git merge main"
```

---

## ❓ FAQ

**Q: Can I see what others are working on?**
A: Yes! `git checkout m3/picking-engine` lets you see M3's code. But DON'T edit anything — just look. Switch back with `git checkout YOUR-BRANCH`.

**Q: What if I accidentally committed to `main`?**
A: Tell Vishal immediately. He can fix it. Don't try to force-push.

**Q: What if Git is confusing and I'm stuck?**
A: Screenshot the error and send it to the team group. Someone will help. Never run `git push --force` — it can delete others' work.

**Q: Do I need an internet connection to use Git?**
A: `git add`, `git commit`, `git status`, `git log` work offline. `git push`, `git pull`, `git clone` need internet.

**Q: What's the difference between `git add` and `git commit`?**
A: `git add` = "I want to include these files in my next save". `git commit` = "Save a snapshot now with a message". Think of it like selecting files (add) and then hitting the save button (commit).

---

## 🆘 Emergency: "I Broke Everything"

If you think you broke the repo:

1. **Don't panic.** Git keeps history of everything.
2. **Don't run `git push --force`** — EVER.
3. **Tell Vishal** with a screenshot of the error.
4. **Worst case nuclear option** (start your branch fresh):
   ```bash
   # Save your code files somewhere safe (copy to Desktop)
   
   # Delete your local repo
   cd ..
   rm -rf OptiGro
   
   # Clone fresh
   git clone https://github.com/Vishal-Jadoun/OptiGro.git
   cd OptiGro
   
   # Create your branch again
   git checkout -b m2/customer-orders
   
   # Copy your saved files back in
   # Commit and push
   ```

---

> **Remember:** Git is a tool to HELP you, not to stress you. The commands above cover 99% of what you need. When in doubt, ask Vishal! 🚀
