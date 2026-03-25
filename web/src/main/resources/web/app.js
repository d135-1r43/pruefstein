// Open a named modal by setting the `open` attribute
function openModal(id) {
  document.getElementById(id)?.setAttribute('open', '');
}

// Populate and open the edit-user modal
function openEditModal(btn) {
  const modal = document.getElementById('edit-user-modal');
  if (!modal) return;

  modal.querySelector('#edit-id').value        = btn.dataset.id;
  modal.querySelector('#edit-firstname').value = btn.dataset.firstname;
  modal.querySelector('#edit-lastname').value  = btn.dataset.lastname;
  modal.querySelector('#edit-mail').value      = btn.dataset.mail;

  modal.setAttribute('open', '');
}

// Populate and open the delete confirmation modal
function openDeleteModal(id) {
  const modal = document.getElementById('delete-user-modal');
  if (!modal) return;
  modal.querySelector('#delete-id').value = id;
  modal.setAttribute('open', '');
}

// Wire up the "Add User" button
document.addEventListener('DOMContentLoaded', () => {
  document.getElementById('open-create-modal')
    ?.addEventListener('click', () => openModal('create-user-modal'));
});
