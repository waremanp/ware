function unlock() {
    const code = document.getElementById('unlockCode').value;
    if (code === '1048') {
        alert('Your files are unlocked!');
        // Add code to decrypt files here
        window.location.href = 'unlock.html';
    } else {
        alert('Incorrect unlock code!');
    }
}